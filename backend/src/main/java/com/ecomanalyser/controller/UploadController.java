package com.ecomanalyser.controller;

import com.ecomanalyser.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Validated
public class UploadController {

    private final ExcelImportService excelImportService;

    @PostMapping("/orders")
    public ResponseEntity<?> uploadOrders(@RequestPart("file") MultipartFile file) throws Exception {
        int count = excelImportService.importOrders(file);
        var warnings = excelImportService.consumeWarnings();
        return ResponseEntity.ok().body(java.util.Map.of(
                "message", "Imported orders: " + count,
                "warnings", warnings,
                "warningCount", warnings.size()
        ));
    }

    @PostMapping("/payments")
    public ResponseEntity<?> uploadPayments(@RequestPart("file") MultipartFile file) throws Exception {
        int count = excelImportService.importPayments(file);
        var warnings = excelImportService.consumeWarnings();
        return ResponseEntity.ok().body(java.util.Map.of(
                "message", "Imported payments: " + count,
                "warnings", warnings,
                "warningCount", warnings.size()
        ));
    }

    @PostMapping("/sku-prices")
    public ResponseEntity<?> uploadSkuPrices(@RequestPart("file") MultipartFile file) throws Exception {
        int count = excelImportService.importSkuPrices(file);
        return ResponseEntity.ok().body(java.util.Map.of("message", "Imported sku prices: " + count));
    }
}


