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
        return ResponseEntity.ok().body("Imported orders: " + count);
    }

    @PostMapping("/payments")
    public ResponseEntity<?> uploadPayments(@RequestPart("file") MultipartFile file) throws Exception {
        int count = excelImportService.importPayments(file);
        return ResponseEntity.ok().body("Imported payments: " + count);
    }

    @PostMapping("/sku-prices")
    public ResponseEntity<?> uploadSkuPrices(@RequestPart("file") MultipartFile file) throws Exception {
        int count = excelImportService.importSkuPrices(file);
        return ResponseEntity.ok().body("Imported sku prices: " + count);
    }
}


