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
            Row row1 = sh.createRow(r++);
            row1.createCell(0).setCellValue("Pillow Cover Pack of 2");
            row1.createCell(1).setCellValue("2-PILLOW-COVER-WHITE-PC-18-28");
            row1.createCell(2).setCellValue(50.00);
            row1.createCell(3).setCellValue("Premium pillow covers in various colors");
            
            Row row2 = sh.createRow(r++);
            row2.createCell(0).setCellValue("Pillow Cover Pack of 2");
            row2.createCell(1).setCellValue("2-PC-PILLOW-C-COFFFEE-STRIP");
            row2.createCell(2).setCellValue(50.00);
            row2.createCell(3).setCellValue("Premium pillow covers in various colors");
            
            Row row3 = sh.createRow(r++);
            row3.createCell(0).setCellValue("Bed Sheets");
            row3.createCell(1).setCellValue("DF-STRIPWINE-90*100");
            row3.createCell(2).setCellValue(172.00);
            row3.createCell(3).setCellValue("High-quality bed sheets");
            
            Row row4 = sh.createRow(r++);
            row4.createCell(0).setCellValue("Bed Sheets");
            row4.createCell(1).setCellValue("1SF-MAROON-FLY-MULTICOLUR");
            row4.createCell(2).setCellValue(150.00);
            row4.createCell(3).setCellValue("Premium cotton bed sheets");
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            byte[] bytes = out.toByteArray();
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=sku_group_template.xlsx")
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(bytes);
        }
    }
}
