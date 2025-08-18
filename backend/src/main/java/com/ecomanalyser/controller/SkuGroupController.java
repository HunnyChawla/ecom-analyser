package com.ecomanalyser.controller;

import com.ecomanalyser.service.SkuGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

@RestController
@RequestMapping("/api/sku-groups")
@RequiredArgsConstructor
@Slf4j
public class SkuGroupController {
    
    private final SkuGroupService skuGroupService;
    
    /**
     * Upload SKU group template
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadSkuGroups(@RequestParam("file") MultipartFile file) {
        try {
            int importedGroups = skuGroupService.importSkuGroups(file);
            return ResponseEntity.ok(Map.of(
                "message", "SKU groups imported successfully",
                "importedGroups", importedGroups
            ));
        } catch (Exception e) {
            log.error("Error importing SKU groups: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to import SKU groups: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get top performing groups by orders
     */
    @GetMapping("/analytics/top-performing")
    public ResponseEntity<List<Map<String, Object>>> getTopPerformingGroups(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            var result = skuGroupService.getTopPerformingGroupsByOrders(start, end);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting top performing groups: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get revenue contribution by group
     */
    @GetMapping("/analytics/revenue-contribution")
    public ResponseEntity<List<Map<String, Object>>> getRevenueContributionByGroup(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            var result = skuGroupService.getRevenueContributionByGroup(start, end);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting revenue contribution by group: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get profit comparison across groups
     */
    @GetMapping("/analytics/profit-comparison")
    public ResponseEntity<List<Map<String, Object>>> getProfitComparisonByGroup(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            var result = skuGroupService.getProfitComparisonByGroup(start, end);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting profit comparison by group: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get all SKU groups
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllSkuGroups() {
        try {
            var groups = skuGroupService.getAllSkuGroups();
            var result = groups.stream().map(group -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", group.getId());
                map.put("groupName", group.getGroupName());
                map.put("purchasePrice", group.getPurchasePrice());
                map.put("description", group.getDescription());
                map.put("createdAt", group.getCreatedAt());
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting SKU groups: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get ungrouped SKUs
     */
    @GetMapping("/ungrouped")
    public ResponseEntity<Map<String, Object>> getUngroupedSkus() {
        try {
            var ungroupedSkus = skuGroupService.getUngroupedSkus();
            return ResponseEntity.ok(Map.of(
                "ungroupedSkus", ungroupedSkus,
                "count", ungroupedSkus.size()
            ));
        } catch (Exception e) {
            log.error("Error getting ungrouped SKUs: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Download SKU group template
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws Exception {
        try (var wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("SkuGroups");
            Row h = sh.createRow(0);
            h.createCell(0).setCellValue("Group Name");
            h.createCell(1).setCellValue("SKU");
            h.createCell(2).setCellValue("Purchase Price");
            h.createCell(3).setCellValue("Description");

            int r = 1;
            var rows = skuGroupService.buildSkuGroupTemplateRows();
            for (String[] vals : rows) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(vals.length > 0 ? vals[0] : "");
                row.createCell(1).setCellValue(vals.length > 1 ? vals[1] : "");
                if (vals.length > 2 && vals[2] != null && !vals[2].isBlank()) {
                    try {
                        row.createCell(2).setCellValue(Double.parseDouble(vals[2]));
                    } catch (NumberFormatException e) {
                        row.createCell(2).setCellValue(vals[2]);
                    }
                } else {
                    row.createCell(2).setCellValue(0d);
                }
                row.createCell(3).setCellValue(vals.length > 3 ? vals[3] : "");
            }

            // Autofit first few columns for readability
            for (int c = 0; c <= 3; c++) sh.autoSizeColumn(c);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            byte[] bytes = out.toByteArray();
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=sku_group_template.xlsx")
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(bytes);
        }
    }

    /**
     * Create a new SKU group
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSkuGroup(@RequestBody Map<String, Object> request) {
        try {
            var group = skuGroupService.createSkuGroup(
                (String) request.get("groupName"),
                Double.parseDouble(request.get("purchasePrice").toString()),
                (String) request.get("description")
            );
            return ResponseEntity.ok(Map.of(
                "message", "SKU group created successfully",
                "group", group
            ));
        } catch (Exception e) {
            log.error("Error creating SKU group: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to create SKU group: " + e.getMessage()
            ));
        }
    }

    /**
     * Update an existing SKU group
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateSkuGroup(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            var group = skuGroupService.updateSkuGroup(
                id,
                (String) request.get("groupName"),
                Double.parseDouble(request.get("purchasePrice").toString()),
                (String) request.get("description")
            );
            return ResponseEntity.ok(Map.of(
                "message", "SKU group updated successfully",
                "group", group
            ));
        } catch (Exception e) {
            log.error("Error updating SKU group: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to update SKU group: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete an SKU group
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteSkuGroup(@PathVariable("id") Long id) {
        try {
            skuGroupService.deleteSkuGroup(id);
            return ResponseEntity.ok(Map.of(
                "message", "SKU group deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Error deleting SKU group: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to delete SKU group: " + e.getMessage()
            ));
        }
    }

    /**
     * Get SKU mappings
     */
    @GetMapping("/mappings")
    public ResponseEntity<List<Map<String, Object>>> getSkuMappings() {
        try {
            var mappings = skuGroupService.getSkuMappings();
            var result = mappings.stream().map(mapping -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", mapping.getId());
                map.put("skuId", mapping.getSku());
                map.put("groupName", mapping.getSkuGroup().getGroupName());
                map.put("groupId", mapping.getSkuGroup().getId());
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting SKU mappings: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Add SKU to group
     */
    @PostMapping("/mappings")
    public ResponseEntity<Map<String, Object>> addSkuToGroup(@RequestBody Map<String, Object> request) {
        try {
            var mapping = skuGroupService.addSkuToGroup(
                (String) request.get("skuId"),
                Long.parseLong(request.get("groupId").toString())
            );
            return ResponseEntity.ok(Map.of(
                "message", "SKU added to group successfully",
                "mapping", mapping
            ));
        } catch (Exception e) {
            log.error("Error adding SKU to group: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to add SKU to group: " + e.getMessage()
            ));
        }
    }

    /**
     * Update SKU group assignment
     */
    @PutMapping("/mappings/{skuId}")
    public ResponseEntity<Map<String, Object>> updateSkuGroupAssignment(
            @PathVariable String skuId,
            @RequestBody Map<String, Object> request) {
        try {
            var mapping = skuGroupService.updateSkuGroupAssignment(
                skuId,
                Long.parseLong(request.get("groupId").toString())
            );
            return ResponseEntity.ok(Map.of(
                "message", "SKU group assignment updated successfully",
                "mapping", mapping
            ));
        } catch (Exception e) {
            log.error("Error updating SKU group assignment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to update SKU group assignment: " + e.getMessage()
            ));
        }
    }
}
