package com.ecomanalyser.service;

import com.ecomanalyser.domain.OrderEntity;
import com.ecomanalyser.domain.PaymentEntity;
import com.ecomanalyser.domain.MergedOrderPaymentEntity;
import com.ecomanalyser.repository.OrderRepository;
import com.ecomanalyser.repository.PaymentRepository;
import com.ecomanalyser.repository.MergedOrderPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataMergeService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final MergedOrderPaymentRepository mergedRepo;

    /**
     * Merged data structure containing combined order and payment information
     */
    public static class MergedOrderData {
        private String orderId;
        private String sku;
        private Integer quantity;
        private BigDecimal sellingPrice;
        private LocalDateTime orderDateTime;
        private String productName;
        private String customerState;
        private String size;
        private BigDecimal supplierListedPrice;
        private BigDecimal supplierDiscountedPrice;
        private String packetId;
        private String reasonForCreditEntry;
        
        // Payment fields
        private String paymentId;
        private BigDecimal amount;
        private LocalDateTime paymentDateTime;
        private String orderStatus;
        private String transactionId;
        private BigDecimal finalSettlementAmount;
        private String priceType;
        private BigDecimal totalSaleAmount;
        private BigDecimal totalSaleReturnAmount;
        private BigDecimal fixedFee;
        private BigDecimal warehousingFee;
        private BigDecimal returnPremium;
        private BigDecimal meeshoCommissionPercentage;
        private BigDecimal meeshoCommission;
        private BigDecimal meeshoGoldPlatformFee;
        private BigDecimal meeshoMallPlatformFee;
        private BigDecimal returnShippingCharge;
        private BigDecimal gstCompensation;
        private BigDecimal shippingCharge;
        private BigDecimal otherSupportServiceCharges;
        private BigDecimal waivers;
        private BigDecimal netOtherSupportServiceCharges;
        private BigDecimal gstOnNetOtherSupportServiceCharges;
        private BigDecimal tcs;
        private BigDecimal tdsRatePercentage;
        private BigDecimal tds;
        private BigDecimal compensation;
        private String compensationReason;
        private BigDecimal claims;
        private String claimsReason;
        private BigDecimal recovery;
        private String recoveryReason;
        private LocalDate dispatchDate;
        private BigDecimal productGstPercentage;
        private BigDecimal listingPriceInclTaxes;
        
        // Computed final status
        private String finalStatus;
        private String statusSource; // "ORDER_FILE", "PAYMENT_FILE", "MERGED"
        
        // Constructor
        public MergedOrderData() {}
        
        // Getters and Setters
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        
        public BigDecimal getSellingPrice() { return sellingPrice; }
        public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
        
        public LocalDateTime getOrderDateTime() { return orderDateTime; }
        public void setOrderDateTime(LocalDateTime orderDateTime) { this.orderDateTime = orderDateTime; }
        
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        
        public String getCustomerState() { return customerState; }
        public void setCustomerState(String customerState) { this.customerState = customerState; }
        
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        
        public BigDecimal getSupplierListedPrice() { return supplierListedPrice; }
        public void setSupplierListedPrice(BigDecimal supplierListedPrice) { this.supplierListedPrice = supplierListedPrice; }
        
        public BigDecimal getSupplierDiscountedPrice() { return supplierDiscountedPrice; }
        public void setSupplierDiscountedPrice(BigDecimal supplierDiscountedPrice) { this.supplierDiscountedPrice = supplierDiscountedPrice; }
        
        public String getPacketId() { return packetId; }
        public void setPacketId(String packetId) { this.packetId = packetId; }
        
        public String getReasonForCreditEntry() { return reasonForCreditEntry; }
        public void setReasonForCreditEntry(String reasonForCreditEntry) { this.reasonForCreditEntry = reasonForCreditEntry; }
        
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public LocalDateTime getPaymentDateTime() { return paymentDateTime; }
        public void setPaymentDateTime(LocalDateTime paymentDateTime) { this.paymentDateTime = paymentDateTime; }
        
        public String getOrderStatus() { return orderStatus; }
        public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
        
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public BigDecimal getFinalSettlementAmount() { return finalSettlementAmount; }
        public void setFinalSettlementAmount(BigDecimal finalSettlementAmount) { this.finalSettlementAmount = finalSettlementAmount; }
        
        public String getPriceType() { return priceType; }
        public void setPriceType(String priceType) { this.priceType = priceType; }
        
        public BigDecimal getTotalSaleAmount() { return totalSaleAmount; }
        public void setTotalSaleAmount(BigDecimal totalSaleAmount) { this.totalSaleAmount = totalSaleAmount; }
        
        public BigDecimal getTotalSaleReturnAmount() { return totalSaleReturnAmount; }
        public void setTotalSaleReturnAmount(BigDecimal totalSaleReturnAmount) { this.totalSaleReturnAmount = totalSaleReturnAmount; }
        
        public BigDecimal getFixedFee() { return fixedFee; }
        public void setFixedFee(BigDecimal fixedFee) { this.fixedFee = fixedFee; }
        
        public BigDecimal getWarehousingFee() { return warehousingFee; }
        public void setWarehousingFee(BigDecimal warehousingFee) { this.warehousingFee = warehousingFee; }
        
        public BigDecimal getReturnPremium() { return returnPremium; }
        public void setReturnPremium(BigDecimal returnPremium) { this.returnPremium = returnPremium; }
        
        public BigDecimal getMeeshoCommissionPercentage() { return meeshoCommissionPercentage; }
        public void setMeeshoCommissionPercentage(BigDecimal meeshoCommissionPercentage) { this.meeshoCommissionPercentage = meeshoCommissionPercentage; }
        
        public BigDecimal getMeeshoCommission() { return meeshoCommission; }
        public void setMeeshoCommission(BigDecimal meeshoCommission) { this.meeshoCommission = meeshoCommission; }
        
        public BigDecimal getMeeshoGoldPlatformFee() { return meeshoGoldPlatformFee; }
        public void setMeeshoGoldPlatformFee(BigDecimal meeshoGoldPlatformFee) { this.meeshoGoldPlatformFee = meeshoGoldPlatformFee; }
        
        public BigDecimal getMeeshoMallPlatformFee() { return meeshoMallPlatformFee; }
        public void setMeeshoMallPlatformFee(BigDecimal meeshoMallPlatformFee) { this.meeshoMallPlatformFee = meeshoMallPlatformFee; }
        
        public BigDecimal getReturnShippingCharge() { return returnShippingCharge; }
        public void setReturnShippingCharge(BigDecimal returnShippingCharge) { this.returnShippingCharge = returnShippingCharge; }
        
        public BigDecimal getGstCompensation() { return gstCompensation; }
        public void setGstCompensation(BigDecimal gstCompensation) { this.gstCompensation = gstCompensation; }
        
        public BigDecimal getShippingCharge() { return shippingCharge; }
        public void setShippingCharge(BigDecimal shippingCharge) { this.shippingCharge = shippingCharge; }
        
        public BigDecimal getOtherSupportServiceCharges() { return otherSupportServiceCharges; }
        public void setOtherSupportServiceCharges(BigDecimal otherSupportServiceCharges) { this.otherSupportServiceCharges = otherSupportServiceCharges; }
        
        public BigDecimal getWaivers() { return waivers; }
        public void setWaivers(BigDecimal waivers) { this.waivers = waivers; }
        
        public BigDecimal getNetOtherSupportServiceCharges() { return netOtherSupportServiceCharges; }
        public void setNetOtherSupportServiceCharges(BigDecimal netOtherSupportServiceCharges) { this.netOtherSupportServiceCharges = netOtherSupportServiceCharges; }
        
        public BigDecimal getGstOnNetOtherSupportServiceCharges() { return gstOnNetOtherSupportServiceCharges; }
        public void setGstOnNetOtherSupportServiceCharges(BigDecimal gstOnNetOtherSupportServiceCharges) { this.gstOnNetOtherSupportServiceCharges = gstOnNetOtherSupportServiceCharges; }
        
        public BigDecimal getTcs() { return tcs; }
        public void setTcs(BigDecimal tcs) { this.tcs = tcs; }
        
        public BigDecimal getTdsRatePercentage() { return tdsRatePercentage; }
        public void setTdsRatePercentage(BigDecimal tdsRatePercentage) { this.tdsRatePercentage = tdsRatePercentage; }
        
        public BigDecimal getTds() { return tds; }
        public void setTds(BigDecimal tds) { this.tds = tds; }
        
        public BigDecimal getCompensation() { return compensation; }
        public void setCompensation(BigDecimal compensation) { this.compensation = compensation; }
        
        public String getCompensationReason() { return compensationReason; }
        public void setCompensationReason(String compensationReason) { this.compensationReason = compensationReason; }
        
        public BigDecimal getClaims() { return claims; }
        public void setClaims(BigDecimal claims) { this.claims = claims; }
        
        public String getClaimsReason() { return claimsReason; }
        public void setClaimsReason(String claimsReason) { this.claimsReason = claimsReason; }
        
        public BigDecimal getRecovery() { return recovery; }
        public void setRecovery(BigDecimal recovery) { this.recovery = recovery; }
        
        public String getRecoveryReason() { return recoveryReason; }
        public void setRecoveryReason(String recoveryReason) { this.recoveryReason = recoveryReason; }
        
        public LocalDate getDispatchDate() { return dispatchDate; }
        public void setDispatchDate(LocalDate dispatchDate) { this.dispatchDate = dispatchDate; }
        
        public BigDecimal getProductGstPercentage() { return productGstPercentage; }
        public void setProductGstPercentage(BigDecimal productGstPercentage) { this.productGstPercentage = productGstPercentage; }
        
        public BigDecimal getListingPriceInclTaxes() { return listingPriceInclTaxes; }
        public void setListingPriceInclTaxes(BigDecimal listingPriceInclTaxes) { this.listingPriceInclTaxes = listingPriceInclTaxes; }
        
        public String getFinalStatus() { return finalStatus; }
        public void setFinalStatus(String finalStatus) { this.finalStatus = finalStatus; }
        
        public String getStatusSource() { return statusSource; }
        public void setStatusSource(String statusSource) { this.statusSource = statusSource; }
    }

    /**
     * Merge orders and payments data with intelligent status resolution
     * 
     * @return List of merged order data
     */
    public List<MergedOrderData> mergeOrdersAndPayments() {
        log.info("Starting merge of orders and payments data...");
        
        try {
            log.info("Fetching all orders...");
            // Fetch all orders and payments
            List<OrderEntity> orders = orderRepository.findAll();
            log.info("Successfully fetched {} orders", orders.size());
            
            log.info("Fetching all payments...");
            List<PaymentEntity> payments = paymentRepository.findAll();
            log.info("Successfully fetched {} payments", payments.size());
            
            log.info("Found {} orders and {} payments", orders.size(), payments.size());
            
            // Check for data consistency
            if (payments.size() > orders.size()) {
                log.warn("WARNING: More payment records ({}) than order records ({}). This may indicate incomplete order data import.", 
                        payments.size(), orders.size());
                log.warn("Missing order records will result in missing SKU, product name, and quantity information.");
            }
            
            log.info("Creating order map...");
            // Create maps for efficient lookup
            Map<String, OrderEntity> orderMap = orders.stream()
                    .collect(Collectors.toMap(OrderEntity::getOrderId, order -> order));
            log.info("Order map created with {} entries", orderMap.size());
            
            log.info("Creating payment map...");
            Map<String, List<PaymentEntity>> paymentMap = payments.stream()
                    .collect(Collectors.groupingBy(PaymentEntity::getOrderId));
            log.info("Payment map created with {} entries", paymentMap.size());
            
            log.info("Collecting all order IDs...");
            Set<String> allOrderIds = new HashSet<>();
            allOrderIds.addAll(orderMap.keySet());
            allOrderIds.addAll(paymentMap.keySet());
            
            log.info("Total unique order IDs: {}", allOrderIds.size());
            
            // Count missing orders
            long missingOrders = paymentMap.keySet().stream()
                    .filter(orderId -> !orderMap.containsKey(orderId))
                    .count();
            if (missingOrders > 0) {
                log.warn("WARNING: {} payment records have no corresponding order records. These will have missing SKU information.", missingOrders);
            }
            
            log.info("Starting merge process...");
            List<MergedOrderData> mergedData = new ArrayList<>();
            
            for (String orderId : allOrderIds) {
                try {
                    OrderEntity order = orderMap.get(orderId);
                    List<PaymentEntity> orderPayments = paymentMap.getOrDefault(orderId, new ArrayList<>());
                    
                    if (orderPayments.isEmpty()) {
                        // Order exists but no payment - use order data only
                        if (order != null) {
                            MergedOrderData merged = createMergedDataFromOrder(order);
                            merged.setStatusSource("ORDER_FILE");
                            mergedData.add(merged);
                            log.debug("Order {} has no payments, using order status: {}", orderId, merged.getFinalStatus());
                        }
                    } else {
                        // Process each payment for this order
                        for (PaymentEntity payment : orderPayments) {
                            MergedOrderData merged = mergeOrderAndPayment(order, payment);
                            mergedData.add(merged);
                            
                            // Log warning for missing SKU
                            if (merged.getSku() == null) {
                                log.warn("WARNING: Order {} has no SKU information. This indicates missing order data.", orderId);
                            }
                            
                            log.debug("Merged order {} with payment {}, final status: {}", 
                                    orderId, payment.getPaymentId(), merged.getFinalStatus());
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing order ID {}: {}", orderId, e.getMessage(), e);
                    // Continue with other orders
                }
            }
            
            log.info("Merge completed. Generated {} merged records", mergedData.size());
            
            // Final summary
            long recordsWithSku = mergedData.stream().filter(r -> r.getSku() != null).count();
            long recordsWithoutSku = mergedData.size() - recordsWithSku;
            log.info("Merge Summary: {} records with SKU, {} records without SKU", recordsWithSku, recordsWithoutSku);
            
            if (recordsWithoutSku > 0) {
                log.warn("RECOMMENDATION: Re-upload the complete order file to resolve missing SKU information for {} records.", recordsWithoutSku);
            }
            
            return mergedData;
        } catch (Exception e) {
            log.error("Error in mergeOrdersAndPayments: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Rebuild merged table from current orders and payments
     */
    public int rebuildMergedTable() {
        log.info("Rebuilding merged_orders with aggregation and status priority rules...");

        // Load all orders and payments
        List<OrderEntity> orders = orderRepository.findAll();
        List<PaymentEntity> payments = paymentRepository.findAll();

        Map<String, OrderEntity> orderById = orders.stream()
                .collect(Collectors.toMap(OrderEntity::getOrderId, o -> o));

        Map<String, List<PaymentEntity>> paymentsByOrder = payments.stream()
                .collect(Collectors.groupingBy(PaymentEntity::getOrderId));

        // All orderIds from either side
        Set<String> allOrderIds = new HashSet<>();
        allOrderIds.addAll(orderById.keySet());
        allOrderIds.addAll(paymentsByOrder.keySet());

        List<MergedOrderPaymentEntity> toPersist = new ArrayList<>(allOrderIds.size());

        for (String orderId : allOrderIds) {
            OrderEntity order = orderById.get(orderId);
            List<PaymentEntity> orderPayments = paymentsByOrder.getOrDefault(orderId, Collections.emptyList());

            // Aggregate settlement amount across all payment rows for this order
            BigDecimal settlementSum = orderPayments.stream()
                    .map(p -> p.getFinalSettlementAmount() != null ? p.getFinalSettlementAmount() : p.getAmount())
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Choose latest payment by paymentDateTime
            PaymentEntity latestPayment = orderPayments.stream()
                    .filter(p -> p.getPaymentDateTime() != null)
                    .max(Comparator.comparing(PaymentEntity::getPaymentDateTime))
                    .orElse(null);

            // Resolve status from payments first: pick first non-blank status scanning by most recent first
            String resolvedStatus = null;
            if (!orderPayments.isEmpty()) {
                List<PaymentEntity> sorted = new ArrayList<>(orderPayments);
                sorted.sort(Comparator.comparing(PaymentEntity::getPaymentDateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                for (PaymentEntity p : sorted) {
                    if (p.getOrderStatus() != null && !p.getOrderStatus().isBlank() && !"unknown".equalsIgnoreCase(p.getOrderStatus())) {
                        resolvedStatus = p.getOrderStatus();
                        break;
                    }
                }
            }
            if (resolvedStatus == null) {
                // Fall back to order's status if available (using reasonForCreditEntry as status surrogate)
                if (order != null && order.getReasonForCreditEntry() != null && !order.getReasonForCreditEntry().isBlank()) {
                    resolvedStatus = order.getReasonForCreditEntry();
                } else {
                    resolvedStatus = "UNKNOWN";
                }
            }

            // Compute other fields
            BigDecimal orderAmount = null;
            Integer quantity = null;
            String sku = null;
            String state = null;
            LocalDate orderDate = null;
            if (order != null) {
                quantity = order.getQuantity();
                sku = order.getSku();
                state = order.getCustomerState();
                orderDate = order.getOrderDateTime() != null ? order.getOrderDateTime().toLocalDate() : null;
                if (order.getSellingPrice() != null && order.getQuantity() != null) {
                    orderAmount = order.getSellingPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
                }
            }

            LocalDate paymentDate = latestPayment != null && latestPayment.getPaymentDateTime() != null
                    ? latestPayment.getPaymentDateTime().toLocalDate()
                    : null;
            // Prefer order_date_time from payments when available, else fallback to orders
            if (latestPayment != null && latestPayment.getOrderDateTime() != null) {
                orderDate = latestPayment.getOrderDateTime().toLocalDate();
            }
            String transactionId = latestPayment != null && latestPayment.getTransactionId() != null && !latestPayment.getTransactionId().isBlank()
                    ? latestPayment.getTransactionId()
                    : orderPayments.stream().map(PaymentEntity::getTransactionId).filter(Objects::nonNull).filter(s -> !s.isBlank()).findFirst().orElse(null);
            String priceType = latestPayment != null ? latestPayment.getPriceType() : null;
            LocalDate dispatchDate = latestPayment != null ? latestPayment.getDispatchDate() : null;

            toPersist.add(MergedOrderPaymentEntity.builder()
                    .orderId(orderId)
                    .orderAmount(orderAmount)
                    .settlementAmount(settlementSum)
                    .orderStatus(resolvedStatus)
                    .skuId(sku)
                    .orderDate(orderDate)
                    .paymentDate(paymentDate)
                    .quantity(quantity)
                    .state(state)
                    .transactionId(transactionId)
                    .dispatchDate(dispatchDate)
                    .priceType(priceType)
                    .build());
        }

        mergedRepo.deleteAllInBatch();
        mergedRepo.saveAll(toPersist);
        log.info("Rebuilt merged_orders with {} rows", toPersist.size());
        return toPersist.size();
    }
    
    /**
     * Create merged data from order only (when no payment exists)
     */
    private MergedOrderData createMergedDataFromOrder(OrderEntity order) {
        MergedOrderData merged = new MergedOrderData();
        
        // Copy order data
        merged.setOrderId(order.getOrderId());
        merged.setSku(order.getSku());
        merged.setQuantity(order.getQuantity());
        merged.setSellingPrice(order.getSellingPrice());
        merged.setOrderDateTime(order.getOrderDateTime());
        merged.setProductName(order.getProductName());
        merged.setCustomerState(order.getCustomerState());
        merged.setSize(order.getSize());
        merged.setSupplierListedPrice(order.getSupplierListedPrice());
        merged.setSupplierDiscountedPrice(order.getSupplierDiscountedPrice());
        merged.setPacketId(order.getPacketId());
        merged.setReasonForCreditEntry(order.getReasonForCreditEntry());
        
        // Set final status from order
        merged.setFinalStatus(order.getReasonForCreditEntry());
        
        return merged;
    }
    
    /**
     * Merge order and payment data with status resolution
     */
    private MergedOrderData mergeOrderAndPayment(OrderEntity order, PaymentEntity payment) {
        MergedOrderData merged = new MergedOrderData();
        
        // Copy order data if available
        if (order != null) {
            merged.setOrderId(order.getOrderId());
            merged.setSku(order.getSku());
            merged.setQuantity(order.getQuantity());
            merged.setSellingPrice(order.getSellingPrice());
            merged.setOrderDateTime(order.getOrderDateTime());
            merged.setProductName(order.getProductName());
            merged.setCustomerState(order.getCustomerState());
            merged.setSize(order.getSize());
            merged.setSupplierListedPrice(order.getSupplierListedPrice());
            merged.setSupplierDiscountedPrice(order.getSupplierDiscountedPrice());
            merged.setPacketId(order.getPacketId());
            merged.setReasonForCreditEntry(order.getReasonForCreditEntry());
        } else {
            // Order doesn't exist, use payment order ID
            merged.setOrderId(payment.getOrderId());
        }
        
        // Copy payment data
        merged.setPaymentId(payment.getPaymentId());
        merged.setAmount(payment.getAmount());
        merged.setPaymentDateTime(payment.getPaymentDateTime());
        merged.setOrderStatus(payment.getOrderStatus());
        merged.setTransactionId(payment.getTransactionId());
        merged.setFinalSettlementAmount(payment.getAmount());
        merged.setPriceType(payment.getPriceType());
        merged.setTotalSaleAmount(payment.getTotalSaleAmount());
        merged.setTotalSaleReturnAmount(payment.getTotalSaleReturnAmount());
        merged.setFixedFee(payment.getFixedFee());
        merged.setWarehousingFee(payment.getWarehousingFee());
        merged.setReturnPremium(payment.getReturnPremium());
        merged.setMeeshoCommissionPercentage(payment.getMeeshoCommissionPercentage());
        merged.setMeeshoCommission(payment.getMeeshoCommission());
        merged.setMeeshoGoldPlatformFee(payment.getMeeshoGoldPlatformFee());
        merged.setMeeshoMallPlatformFee(payment.getMeeshoMallPlatformFee());
        merged.setReturnShippingCharge(payment.getReturnShippingCharge());
        merged.setGstCompensation(payment.getGstCompensation());
        merged.setShippingCharge(payment.getShippingCharge());
        merged.setOtherSupportServiceCharges(payment.getOtherSupportServiceCharges());
        merged.setWaivers(payment.getWaivers());
        merged.setNetOtherSupportServiceCharges(payment.getNetOtherSupportServiceCharges());
        merged.setGstOnNetOtherSupportServiceCharges(payment.getGstOnNetOtherSupportServiceCharges());
        merged.setTcs(payment.getTcs());
        merged.setTdsRatePercentage(payment.getTdsRatePercentage());
        merged.setTds(payment.getTds());
        merged.setCompensation(payment.getCompensation());
        merged.setCompensationReason(payment.getCompensationReason());
        merged.setClaims(payment.getClaims());
        merged.setClaimsReason(payment.getClaimsReason());
        merged.setRecovery(payment.getRecovery());
        merged.setRecoveryReason(payment.getRecoveryReason());
        merged.setDispatchDate(payment.getDispatchDate());
        merged.setProductGstPercentage(payment.getProductGstPercentage());
        merged.setListingPriceInclTaxes(payment.getListingPriceInclTaxes());
        
        // Resolve final status based on priority rules
        resolveFinalStatus(merged, order, payment);
        
        return merged;
    }
    
    /**
     * Resolve final status based on priority rules
     */
    private void resolveFinalStatus(MergedOrderData merged, OrderEntity order, PaymentEntity payment) {
        String paymentStatus = payment.getOrderStatus();
        String orderStatus = order != null ? order.getReasonForCreditEntry() : null;
        
        if (paymentStatus != null && !paymentStatus.trim().isEmpty() && !"unknown".equalsIgnoreCase(paymentStatus)) {
            // Payment status is not blank and not "unknown" - use it as final status
            merged.setFinalStatus(paymentStatus);
            merged.setStatusSource("PAYMENT_FILE");
            log.debug("Using payment status for order {}: {}", merged.getOrderId(), paymentStatus);
        } else if (orderStatus != null && !orderStatus.trim().isEmpty()) {
            // Payment status is blank/unknown, use order status
            merged.setFinalStatus(orderStatus);
            merged.setStatusSource("ORDER_FILE");
            log.debug("Using order status for order {}: {}", merged.getOrderId(), orderStatus);
        } else {
            // Both are blank/null
            merged.setFinalStatus("UNKNOWN");
            merged.setStatusSource("MERGED");
            log.warn("Both order and payment status are blank for order: {}", merged.getOrderId());
        }
    }
    
    /**
     * Get merge statistics for monitoring
     */
    public Map<String, Object> getMergeStatistics() {
        List<MergedOrderData> mergedData = mergeOrdersAndPayments();
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalMergedRecords", mergedData.size());
        
        // Count by status source (handle null values)
        Map<String, Long> statusSourceCounts = mergedData.stream()
                .map(record -> record.getStatusSource() != null ? record.getStatusSource() : "UNKNOWN")
                .collect(Collectors.groupingBy(source -> source, Collectors.counting()));
        stats.put("statusSourceBreakdown", statusSourceCounts);
        
        // Count by final status (handle null values)
        Map<String, Long> finalStatusCounts = mergedData.stream()
                .map(record -> record.getFinalStatus() != null ? record.getFinalStatus() : "UNKNOWN")
                .collect(Collectors.groupingBy(status -> status, Collectors.counting()));
        stats.put("finalStatusBreakdown", finalStatusCounts);
        
        // Count unique orders
        long uniqueOrders = mergedData.stream()
                .map(MergedOrderData::getOrderId)
                .distinct()
                .count();
        stats.put("uniqueOrders", uniqueOrders);
        
        // Data quality metrics
        long recordsWithSku = mergedData.stream().filter(r -> r.getSku() != null).count();
        long recordsWithoutSku = mergedData.size() - recordsWithSku;
        long recordsWithProductName = mergedData.stream().filter(r -> r.getProductName() != null).count();
        long recordsWithQuantity = mergedData.stream().filter(r -> r.getQuantity() != null).count();
        
        Map<String, Object> dataQuality = new HashMap<>();
        dataQuality.put("recordsWithSku", recordsWithSku);
        dataQuality.put("recordsWithoutSku", recordsWithoutSku);
        dataQuality.put("skuCoveragePercentage", Math.round((double) recordsWithSku / mergedData.size() * 100));
        dataQuality.put("recordsWithProductName", recordsWithProductName);
        dataQuality.put("recordsWithQuantity", recordsWithQuantity);
        
        stats.put("dataQuality", dataQuality);
        
        // Warning if SKU coverage is low
        if (recordsWithoutSku > 0) {
            stats.put("warning", String.format("WARNING: %d records (%.1f%%) are missing SKU information. Consider re-uploading the complete order file.", 
                    recordsWithoutSku, (double) recordsWithoutSku / mergedData.size() * 100));
        }
        
        return stats;
    }
}
