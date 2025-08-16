package com.ecomanalyser.controller;

import com.ecomanalyser.repository.OrderRepository;
import com.ecomanalyser.repository.SkuPriceRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sku-prices")
@RequiredArgsConstructor
public class PriceTemplateController {

    private final OrderRepository orderRepository;
    private final SkuPriceRepository skuPriceRepository;

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws Exception {
        var orders = orderRepository.findAll();
        var uniqueSkus = orders.stream().map(o -> o.getSku()).collect(Collectors.toCollection(LinkedHashSet::new));

        try (var wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("SkuPrices");
            Row h = sh.createRow(0);
            h.createCell(0).setCellValue("sku");
            h.createCell(1).setCellValue("purchasePrice");
            int r = 1;
            for (String sku : uniqueSkus) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(sku);
                
                // Get existing purchase price from database, or 0.0 if not found
                BigDecimal existingPrice = skuPriceRepository.findBySku(sku)
                        .map(sp -> sp.getPurchasePrice())
                        .orElse(BigDecimal.ZERO);
                
                row.createCell(1).setCellValue(existingPrice.doubleValue());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            byte[] bytes = out.toByteArray();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sku_price_template.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        }
    }
}


