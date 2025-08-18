package com.ecomanalyser.service;

import com.ecomanalyser.domain.SkuGroupEntity;
import com.ecomanalyser.domain.SkuGroupMappingEntity;
import com.ecomanalyser.domain.OrderEntity;
import com.ecomanalyser.domain.PaymentEntity;
import com.ecomanalyser.domain.MergedOrderPaymentEntity;
import com.ecomanalyser.repository.SkuGroupRepository;
import com.ecomanalyser.repository.SkuGroupMappingRepository;
import com.ecomanalyser.repository.OrderRepository;
import com.ecomanalyser.repository.PaymentRepository;
import com.ecomanalyser.repository.SkuPriceRepository;
import com.ecomanalyser.repository.MergedOrderPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkuGroupService {
    
    private final SkuGroupRepository skuGroupRepository;
    private final SkuGroupMappingRepository skuGroupMappingRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SkuPriceRepository skuPriceRepository;
    private final MergedOrderPaymentRepository mergedOrderRepository;
    
    /**
     * Import SKU groups from Excel template
     */
    @Transactional
    public int importSkuGroups(MultipartFile file) throws IOException {
        log.info("Starting SKU group import from file: {}", file.getOriginalFilename());
        
        // Clear existing groups and mappings
        skuGroupMappingRepository.deleteAllInBatch();
        skuGroupRepository.deleteAllInBatch();
        
        int importedGroups = 0;
        int importedMappings = 0;
        
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream())) {
            var sheet = workbook.getSheetAt(0);
            
            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row == null) continue;
                
                String groupName = getCellValue(row, 0);
                String sku = getCellValue(row, 1);
                String priceStr = getCellValue(row, 2);
                String description = getCellValue(row, 3);
                
                if (groupName == null || groupName.trim().isEmpty() || 
                    sku == null || sku.trim().isEmpty() || 
                    priceStr == null || priceStr.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    BigDecimal price = new BigDecimal(priceStr.trim());
                    
                    // Create or get existing group
                    SkuGroupEntity group = skuGroupRepository.findByGroupName(groupName.trim())
                            .orElse(SkuGroupEntity.builder()
                                    .groupName(groupName.trim())
                                    .purchasePrice(price)
                                    .description(description != null ? description.trim() : "")
                                    .build());
                    
                    // Update price if group exists
                    if (group.getId() != null) {
                        group.setPurchasePrice(price);
                        if (description != null && !description.trim().isEmpty()) {
                            group.setDescription(description.trim());
                        }
                    }
                    
                    group = skuGroupRepository.save(group);
                    importedGroups++;
                    
                    // Create SKU mapping
                    SkuGroupMappingEntity mapping = SkuGroupMappingEntity.builder()
                            .sku(sku.trim())
                            .skuGroup(group)
                            .build();
                    
                    skuGroupMappingRepository.save(mapping);
                    importedMappings++;
                    
                } catch (NumberFormatException e) {
                    log.warn("Invalid price format for group {}: {}", groupName, priceStr);
                }
            }
        }
        
        log.info("SKU group import completed: {} groups, {} mappings", importedGroups, importedMappings);
        return importedGroups;
    }
    
    /**
     * Get purchase price for a SKU (from group or individual price)
     */
    public BigDecimal getPurchasePriceForSku(String sku) {
        // First try to get price from group mapping
        Optional<String> groupName = skuGroupMappingRepository.findGroupNameBySku(sku);
        if (groupName.isPresent()) {
            Optional<SkuGroupEntity> group = skuGroupRepository.findByGroupName(groupName.get());
            if (group.isPresent()) {
                return group.get().getPurchasePrice();
            }
        }
        
        // Fallback to individual SKU price
        return skuPriceRepository.findBySku(sku)
                .map(sp -> sp.getPurchasePrice())
                .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Get group analytics - top performing groups by orders
     * Uses merged_orders table for accurate revenue and profit calculations
     */
    public List<Map<String, Object>> getTopPerformingGroupsByOrders(LocalDate start, LocalDate end) {
        // Use merged_orders table for accurate data
        var mergedOrders = mergedOrderRepository.findByOrderDateBetween(start, end);
        
        // Group orders by SKU group
        Map<String, GroupAnalytics> groupAnalytics = new HashMap<>();
        
        for (MergedOrderPaymentEntity merged : mergedOrders) {
            if (merged.getSkuId() == null || merged.getSettlementAmount() == null || 
                merged.getSettlementAmount().compareTo(BigDecimal.ZERO) <= 0) {
                continue; // Skip invalid records
            }
            
            String groupName = skuGroupMappingRepository.findGroupNameBySku(merged.getSkuId())
                    .orElse("Ungrouped SKUs");
            
            GroupAnalytics analytics = groupAnalytics.computeIfAbsent(groupName, 
                k -> new GroupAnalytics(k, BigDecimal.ZERO, 0L, BigDecimal.ZERO));
            
            analytics.orderCount++;
            analytics.totalQuantity += merged.getQuantity() != null ? merged.getQuantity() : 0;
            // Use actual settlement amount (real revenue) instead of listed price
            analytics.totalRevenue = analytics.totalRevenue.add(merged.getSettlementAmount());
        }
        
        // Calculate profit for each group using accurate cost calculation
        for (GroupAnalytics analytics : groupAnalytics.values()) {
            if (!"Ungrouped SKUs".equals(analytics.groupName)) {
                // Calculate total cost using weighted average of purchase prices
                BigDecimal totalCost = calculateGroupTotalCost(analytics.groupName, analytics.totalQuantity);
                analytics.totalProfit = analytics.totalRevenue.subtract(totalCost);
            }
        }
        
        // Sort by order count and return top 10
        return groupAnalytics.values().stream()
                .sorted(Comparator.comparingLong((GroupAnalytics a) -> a.orderCount).reversed())
                .limit(10)
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }
    
    /**
     * Get revenue contribution by group
     * Uses merged_orders table for accurate revenue calculations
     */
    public List<Map<String, Object>> getRevenueContributionByGroup(LocalDate start, LocalDate end) {
        // Use merged_orders table for accurate data
        var mergedOrders = mergedOrderRepository.findByOrderDateBetween(start, end);
        
        // Group orders by SKU group
        Map<String, BigDecimal> groupRevenue = new HashMap<>();
        
        for (MergedOrderPaymentEntity merged : mergedOrders) {
            if (merged.getSkuId() == null || merged.getSettlementAmount() == null || 
                merged.getSettlementAmount().compareTo(BigDecimal.ZERO) <= 0) {
                continue; // Skip invalid records
            }
            
            String groupName = skuGroupMappingRepository.findGroupNameBySku(merged.getSkuId())
                    .orElse("Ungrouped SKUs");
            
            // Use actual settlement amount (real revenue) instead of listed price
            BigDecimal revenue = merged.getSettlementAmount();
            groupRevenue.merge(groupName, revenue, BigDecimal::add);
        }
        
        // Convert to list and sort by revenue
        return groupRevenue.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("groupName", entry.getKey());
                    map.put("revenue", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Get profit comparison across groups
     * Uses merged_orders table for accurate revenue and profit calculations
     */
    public List<Map<String, Object>> getProfitComparisonByGroup(LocalDate start, LocalDate end) {
        // Use merged_orders table for accurate data
        var mergedOrders = mergedOrderRepository.findByOrderDateBetween(start, end);
        
        // Group orders by SKU group
        Map<String, GroupAnalytics> groupAnalytics = new HashMap<>();
        
        for (MergedOrderPaymentEntity merged : mergedOrders) {
            if (merged.getSkuId() == null || merged.getSettlementAmount() == null || 
                merged.getSettlementAmount().compareTo(BigDecimal.ZERO) <= 0) {
                continue; // Skip invalid records
            }
            
            String groupName = skuGroupMappingRepository.findGroupNameBySku(merged.getSkuId())
                    .orElse("Ungrouped SKUs");
            
            GroupAnalytics analytics = groupAnalytics.computeIfAbsent(groupName, 
                k -> new GroupAnalytics(k, BigDecimal.ZERO, 0L, BigDecimal.ZERO));
            
            analytics.orderCount++;
            analytics.totalQuantity += merged.getQuantity() != null ? merged.getQuantity() : 0;
            // Use actual settlement amount (real revenue) instead of listed price
            analytics.totalRevenue = analytics.totalRevenue.add(merged.getSettlementAmount());
        }
        
        // Calculate profit for each group using accurate cost calculation
        for (GroupAnalytics analytics : groupAnalytics.values()) {
            if (!"Ungrouped SKUs".equals(analytics.groupName)) {
                // Calculate total cost using weighted average of purchase prices
                BigDecimal totalCost = calculateGroupTotalCost(analytics.groupName, analytics.totalQuantity);
                analytics.totalProfit = analytics.totalRevenue.subtract(totalCost);
            }
        }
        
        // Sort by profit and return
        return groupAnalytics.values().stream()
                .sorted(Comparator.comparing((GroupAnalytics a) -> a.totalProfit).reversed())
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all SKU groups
     */
    public List<SkuGroupEntity> getAllSkuGroups() {
        return skuGroupRepository.findAll();
    }
    
    /**
     * Get ungrouped SKUs
     */
    public List<String> getUngroupedSkus() {
        var allSkus = mergedOrderRepository.findAll().stream()
                .map(MergedOrderPaymentEntity::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toSet());
        
        var groupedSkus = skuGroupMappingRepository.findAll().stream()
                .map(SkuGroupMappingEntity::getSku)
                .collect(Collectors.toSet());
        
        return allSkus.stream()
                .filter(sku -> !groupedSkus.contains(sku))
                .collect(Collectors.toList());
    }

    /**
     * Create a new SKU group
     */
    @Transactional
    public SkuGroupEntity createSkuGroup(String groupName, double purchasePrice, String description) {
        SkuGroupEntity group = SkuGroupEntity.builder()
                .groupName(groupName)
                .purchasePrice(BigDecimal.valueOf(purchasePrice))
                .description(description != null ? description : "")
                .createdAt(LocalDateTime.now())
                .build();
        
        return skuGroupRepository.save(group);
    }

    /**
     * Update an existing SKU group
     */
    @Transactional
    public SkuGroupEntity updateSkuGroup(Long id, String groupName, double purchasePrice, String description) {
        SkuGroupEntity group = skuGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SKU group not found with id: " + id));
        
        group.setGroupName(groupName);
        group.setPurchasePrice(BigDecimal.valueOf(purchasePrice));
        group.setDescription(description != null ? description : "");
        
        return skuGroupRepository.save(group);
    }

    /**
     * Delete an SKU group
     */
    @Transactional
    public void deleteSkuGroup(Long id) {
        // First delete all mappings for this group
        List<SkuGroupMappingEntity> mappings = skuGroupMappingRepository.findBySkuGroupId(id);
        skuGroupMappingRepository.deleteAll(mappings);
        
        // Then delete the group
        skuGroupRepository.deleteById(id);
    }

    /**
     * Get all SKU mappings
     */
    public List<SkuGroupMappingEntity> getSkuMappings() {
        return skuGroupMappingRepository.findAllWithGroupDetails();
    }

    /**
     * Add SKU to group
     */
    @Transactional
    public SkuGroupMappingEntity addSkuToGroup(String skuId, Long groupId) {
        SkuGroupEntity group = skuGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("SKU group not found with id: " + groupId));
        
        // Check if SKU is already mapped
        Optional<SkuGroupMappingEntity> existingMapping = skuGroupMappingRepository.findBySku(skuId);
        if (existingMapping.isPresent()) {
            throw new RuntimeException("SKU " + skuId + " is already mapped to a group");
        }
        
        SkuGroupMappingEntity mapping = SkuGroupMappingEntity.builder()
                .sku(skuId)
                .skuGroup(group)
                .build();
        
        return skuGroupMappingRepository.save(mapping);
    }

    /**
     * Update SKU group assignment
     */
    @Transactional
    public SkuGroupMappingEntity updateSkuGroupAssignment(String skuId, Long groupId) {
        SkuGroupEntity group = skuGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("SKU group not found with id: " + groupId));
        
        // Find existing mapping
        SkuGroupMappingEntity existingMapping = skuGroupMappingRepository.findBySku(skuId)
                .orElseThrow(() -> new RuntimeException("SKU mapping not found for: " + skuId));
        
        // Update the group
        existingMapping.setSkuGroup(group);
        
        return skuGroupMappingRepository.save(existingMapping);
    }
    
    // Helper methods
    private String getCellValue(org.apache.poi.ss.usermodel.Row row, int index) {
        var cell = row.getCell(index);
        return cell != null ? cell.toString() : null;
    }
    
    /**
     * Calculate total cost for a group using weighted average of purchase prices
     */
    private BigDecimal calculateGroupTotalCost(String groupName, long totalQuantity) {
        try {
            Optional<SkuGroupEntity> group = skuGroupRepository.findByGroupName(groupName);
            if (group.isPresent() && group.get().getPurchasePrice() != null) {
                return group.get().getPurchasePrice().multiply(BigDecimal.valueOf(totalQuantity));
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Error calculating cost for group {}: {}", groupName, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    private Map<String, Object> convertToMap(GroupAnalytics analytics) {
        Map<String, Object> map = new HashMap<>();
        map.put("groupName", analytics.groupName);
        map.put("orderCount", analytics.orderCount);
        map.put("totalQuantity", analytics.totalQuantity);
        map.put("totalRevenue", analytics.totalRevenue);
        map.put("totalProfit", analytics.totalProfit);
        return map;
    }
    
    /**
     * Build rows for the SKU group template including all grouped and ungrouped SKUs
     * Row format: [groupName, sku, purchasePrice, description]
     */
    public List<String[]> buildSkuGroupTemplateRows() {
        List<String[]> rows = new ArrayList<>();
        // Existing groups with their SKUs
        var groups = skuGroupRepository.findAll();
        for (SkuGroupEntity group : groups) {
            var skus = skuGroupMappingRepository.findSkusByGroupId(group.getId());
            for (String sku : skus) {
                rows.add(new String[] {
                        group.getGroupName(),
                        sku,
                        group.getPurchasePrice() != null ? group.getPurchasePrice().toPlainString() : "",
                        group.getDescription() != null ? group.getDescription() : ""
                });
            }
        }
        
        // Ungrouped SKUs (leave group and price empty so users can fill in)
        var ungrouped = getUngroupedSkus();
        for (String sku : ungrouped) {
            rows.add(new String[] { "", sku, "", "" });
        }
        return rows;
    }
    
    // Inner class for analytics data
    private static class GroupAnalytics {
        String groupName;
        BigDecimal totalRevenue;
        long totalQuantity;
        BigDecimal totalProfit;
        long orderCount;
        
        GroupAnalytics(String groupName, BigDecimal totalRevenue, long totalQuantity, BigDecimal totalProfit) {
            this.groupName = groupName;
            this.totalRevenue = totalRevenue;
            this.totalQuantity = totalQuantity;
            this.totalProfit = totalProfit;
            this.orderCount = 0L;
        }
    }
}
