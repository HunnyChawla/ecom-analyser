package com.ecomanalyser.config;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@Configuration
public class SampleDataGenerator {

    @Value("${sample.generate:true}")
    private boolean generate;

    @Bean
    CommandLineRunner generateSamplesIfMissing() {
        return args -> {
            if (!generate) return;
            Path dataDir = Path.of("..", "data");
            Files.createDirectories(dataDir);

            createOrdersSample(dataDir.resolve("sample_orders.xlsx").toFile());
            createPaymentsSample(dataDir.resolve("sample_payments.xlsx").toFile());
            createSkuPricesSample(dataDir.resolve("sample_sku_prices.xlsx").toFile());
        };
    }

    private void createOrdersSample(File file) throws Exception {
        if (file.exists()) return;
        try (var wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Orders");
            Row h = sh.createRow(0);
            h.createCell(0).setCellValue("orderId");
            h.createCell(1).setCellValue("sku");
            h.createCell(2).setCellValue("quantity");
            h.createCell(3).setCellValue("sellingPrice");
            h.createCell(4).setCellValue("orderDate");
            Object[][] rows = new Object[][]{
                    {"O-1001","SKU-1",2, 120.0, LocalDate.now().minusDays(7).toString()},
                    {"O-1002","SKU-2",1, 200.0, LocalDate.now().minusDays(6).toString()},
                    {"O-1003","SKU-1",3, 115.0, LocalDate.now().minusDays(3).toString()},
                    {"O-1004","SKU-3",2, 80.0,  LocalDate.now().minusDays(1).toString()},
            };
            for (int i = 0; i < rows.length; i++) {
                Row r = sh.createRow(i + 1);
                r.createCell(0).setCellValue((String) rows[i][0]);
                r.createCell(1).setCellValue((String) rows[i][1]);
                r.createCell(2).setCellValue((Integer) rows[i][2]);
                r.createCell(3).setCellValue((Double) rows[i][3]);
                r.createCell(4).setCellValue((String) rows[i][4]);
            }
            try (var fos = new FileOutputStream(file)) { wb.write(fos); }
        }
    }

    private void createPaymentsSample(File file) throws Exception {
        if (file.exists()) return;
        try (var wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Payments");
            Row h = sh.createRow(0);
            h.createCell(0).setCellValue("paymentId");
            h.createCell(1).setCellValue("orderId");
            h.createCell(2).setCellValue("amount");
            h.createCell(3).setCellValue("paymentDate");
            Object[][] rows = new Object[][]{
                    {"P-5001","O-1001",240.0, LocalDate.now().minusDays(6).toString()},
                    {"P-5002","O-1002",200.0, LocalDate.now().minusDays(5).toString()},
                    {"P-5003","O-1003",345.0, LocalDate.now().minusDays(2).toString()},
                    {"P-5004","O-1004",160.0, LocalDate.now().toString()},
            };
            for (int i = 0; i < rows.length; i++) {
                Row r = sh.createRow(i + 1);
                r.createCell(0).setCellValue((String) rows[i][0]);
                r.createCell(1).setCellValue((String) rows[i][1]);
                r.createCell(2).setCellValue((Double) rows[i][2]);
                r.createCell(3).setCellValue((String) rows[i][3]);
            }
            try (var fos = new FileOutputStream(file)) { wb.write(fos); }
        }
    }

    private void createSkuPricesSample(File file) throws Exception {
        if (file.exists()) return;
        try (var wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("SkuPrices");
            Row h = sh.createRow(0);
            h.createCell(0).setCellValue("sku");
            h.createCell(1).setCellValue("purchasePrice");
            Object[][] rows = new Object[][]{
                    {"SKU-1", 90.0},
                    {"SKU-2", 150.0},
                    {"SKU-3", 70.0},
            };
            for (int i = 0; i < rows.length; i++) {
                Row r = sh.createRow(i + 1);
                r.createCell(0).setCellValue((String) rows[i][0]);
                r.createCell(1).setCellValue((Double) rows[i][1]);
            }
            try (var fos = new FileOutputStream(file)) { wb.write(fos); }
        }
    }
}


