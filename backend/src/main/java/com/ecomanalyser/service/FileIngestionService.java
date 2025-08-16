package com.ecomanalyser.service;

import com.ecomanalyser.domain.OrderRawEntity;
import com.ecomanalyser.domain.PaymentRawEntity;
import com.ecomanalyser.dto.IngestionResponse;
import com.ecomanalyser.event.FileIngestedEvent;
import com.ecomanalyser.repository.OrderRawRepository;
import com.ecomanalyser.repository.PaymentRawRepository;
import com.ecomanalyser.service.SchemaValidationService.SchemaValidationResult;
import com.ecomanalyser.event.FileIngestedEvent.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileIngestionService {
    
    private final OrderRawRepository orderRawRepository;
    private final PaymentRawRepository paymentRawRepository;
    private final SchemaValidationService schemaValidationService;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public IngestionResponse ingestFile(MultipartFile file, FileType fileType) {
        log.info("Starting file ingestion for type: {}, file: {}", fileType, file.getOriginalFilename());
        
        String batchId = generateBatchId(fileType);
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        try {
            if (isCsv(file)) {
                return ingestCsvFile(file, fileType, batchId);
            } else {
                return ingestExcelFile(file, fileType, batchId);
            }
        } catch (Exception e) {
            log.error("Error during file ingestion: {}", e.getMessage(), e);
            errors.add("File processing failed: " + e.getMessage());
            
            return IngestionResponse.builder()
                    .batchId(batchId)
                    .acceptedRows(0)
                    .rejectedRows(0)
                    .warningsCount(warnings.size())
                    .ingestedAt(LocalDateTime.now())
                    .warnings(warnings)
                    .errors(errors)
                    .build();
        }
    }
    
    private IngestionResponse ingestCsvFile(MultipartFile file, FileType fileType, String batchId) throws Exception {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            
            // Validate schema
            Set<String> actualColumns = parser.getHeaderMap().keySet();
            SchemaValidationResult validationResult = schemaValidationService.validateSchema(actualColumns, fileType);
            
            warnings.addAll(validationResult.getWarnings());
            errors.addAll(validationResult.getErrors());
            
            if (!validationResult.isValid()) {
                log.warn("Schema validation failed for batch {}: {}", batchId, errors);
                return createRejectionResponse(batchId, warnings, errors);
            }
            
            // Process rows
            List<CSVRecord> records = parser.getRecords();
            return processRows(fileType, batchId, records, warnings, errors, file);
        }
    }
    
    private IngestionResponse ingestExcelFile(MultipartFile file, FileType fileType, String batchId) throws Exception {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = findCorrectSheet(wb, fileType);
            if (sheet == null) {
                errors.add("No valid sheet found in Excel file");
                return createRejectionResponse(batchId, warnings, errors);
            }
            
            log.info("Using sheet: {}", sheet.getSheetName());
            
            // Find the correct header row by searching for expected columns
            Row headerRow = findHeaderRow(sheet, fileType);
            if (headerRow == null) {
                errors.add("No valid header row found in Excel file");
                return createRejectionResponse(batchId, warnings, errors);
            }
            
            Set<String> actualColumns = new HashSet<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String cellValue = getCellAsString(cell);
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        actualColumns.add(cellValue.trim());
                    }
                }
            }
            
            log.info("Detected columns in Excel file: {}", actualColumns);
            
            // Validate schema
            SchemaValidationResult validationResult = schemaValidationService.validateSchema(actualColumns, fileType);
            warnings.addAll(validationResult.getWarnings());
            errors.addAll(validationResult.getErrors());
            
            if (!validationResult.isValid()) {
                log.warn("Schema validation failed for batch {}: {}", batchId, errors);
                return createRejectionResponse(batchId, warnings, errors);
            }
            
            // Process rows starting from the row after the header
            int headerRowNum = headerRow.getRowNum();
            List<Row> dataRows = new ArrayList<>();
            
            // For payments: header is at row 2 (1-based), data starts from row 4 (1-based)
            // In 0-based indexing: header at index 1, data starts at index 3
            int dataStartRow = fileType == FileType.PAYMENTS ? headerRowNum + 2 : headerRowNum + 1;
            
            for (int i = dataStartRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null && !isEmptyRow(row)) {
                    dataRows.add(row);
                }
            }
            
            log.info("Found {} data rows starting from row {} (1-based) (header was at row {} 1-based)", 
                    dataRows.size(), dataStartRow + 1, headerRowNum + 1);
            
            return processExcelRows(fileType, batchId, dataRows, warnings, errors, file);
        }
    }
    
    private Sheet findCorrectSheet(Workbook wb, FileType fileType) {
        // For payments, look for "Order Payments" sheet specifically
        if (fileType == FileType.PAYMENTS) {
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                String sheetName = sheet.getSheetName().toLowerCase();
                log.info("Checking sheet: {}", sheet.getSheetName());
                
                if (sheetName.contains("order") && sheetName.contains("payment")) {
                    log.info("Found Order Payments sheet: {}", sheet.getSheetName());
                    return sheet;
                }
            }
            log.warn("No 'Order Payments' sheet found, using first sheet");
        }
        
        // Default to first sheet
        return wb.getSheetAt(0);
    }
    
    private Row findHeaderRow(Sheet sheet, FileType fileType) {
        // Get expected columns for this file type
        Set<String> expectedColumns = getExpectedColumnsForFileType(fileType);
        
        log.info("Looking for header row in sheet: {}", sheet.getSheetName());
        
        // Search through first 20 rows to find the header (increased from 10)
        int maxRowsToCheck = Math.min(20, sheet.getLastRowNum() + 1);
        for (int rowNum = 0; rowNum < maxRowsToCheck; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                continue;
            }
            
            Set<String> rowColumns = new HashSet<>();
            for (int i = 0; i < row.getLastCellNum(); i++) {
                Cell cell = row.getCell(i);
                if (cell != null) {
                    String cellValue = getCellAsString(cell);
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        rowColumns.add(cellValue.trim().toLowerCase());
                    }
                }
            }
            
            // Check if this row contains enough expected columns to be a header
            long matchingColumns = expectedColumns.stream()
                    .map(String::toLowerCase)
                    .filter(rowColumns::contains)
                    .count();
            
            // Lower the threshold to 2 matching columns for more flexibility
            if (matchingColumns >= 2) { // Changed from 3 to 2
                log.info("Found header row at position {} with {} matching columns", rowNum, matchingColumns);
                return row;
            }
        }
        
        log.warn("No valid header row found in first {} rows", maxRowsToCheck);
        return null;
    }
    
    private Set<String> getExpectedColumnsForFileType(FileType fileType) {
        return switch (fileType) {
            case ORDERS -> Set.of(
                "reason for credit entry", "sub order no", "order date", "customer state",
                "product name", "sku", "size", "quantity", "supplier listed price (incl. gst + commission)",
                "supplier discounted price (incl gst and commision)", "packet id"
            );
            case PAYMENTS -> Set.of(
                "sub order no", "order date", "dispatch date", "product name", "supplier sku",
                "live order status", "product gst %", "listing price (incl. taxes)", "quantity",
                "transaction id", "payment date", "final settlement amount", "price type",
                "total sale amount (incl. shipping & gst)", "total sale return amount (incl. shipping & gst)",
                "fixed fee (incl. gst)", "warehousing fee (incl. gst)", "return premium (incl. gst)",
                "return premium (incl. gst) of return", "meesho commission percentage",
                "meesho commission (incl. gst)", "meesho gold platform fee (incl. gst)",
                "meesho mall platform fee (incl. gst)", "return shipping charge (incl. gst)",
                "gst compensation (prp shipping)", "shipping charge (incl. gst)",
                "other support service charges (excl. gst)", "waivers (excl. gst)",
                "net other support service charges (excl. gst)", "gst on net other support service charges",
                "tcs", "tds rate %", "tds", "compensation", "compensation reason", "claims",
                "claims reason", "recovery", "recovery reason"
            );
        };
    }
    
    private String getCellAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return cell.getCellFormula();
                    }
                }
            default:
                return "";
        }
    }
    
    private boolean isEmptyRow(Row row) {
        if (row == null) return true;
        
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null) {
                String value = getCellAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private IngestionResponse processRows(FileType fileType, String batchId, List<CSVRecord> records, 
                                        List<String> warnings, List<String> errors, MultipartFile file) {
        int acceptedRows = 0;
        int rejectedRows = 0;
        
        for (int i = 0; i < records.size(); i++) {
            CSVRecord record = records.get(i);
            String rawData = record.toString();
            
            try {
                if (fileType == FileType.ORDERS) {
                    OrderRawEntity rawEntity = OrderRawEntity.builder()
                            .batchId(batchId)
                            .rowNumber(i + 1)
                            .rawData(rawData)
                            .validationStatus(OrderRawEntity.ValidationStatus.VALID)
                            .processed(false)
                            .build();
                    orderRawRepository.save(rawEntity);
                    acceptedRows++;
                } else {
                    PaymentRawEntity rawEntity = PaymentRawEntity.builder()
                            .batchId(batchId)
                            .rowNumber(i + 1)
                            .rawData(rawData)
                            .validationStatus(PaymentRawEntity.ValidationStatus.VALID)
                            .processed(false)
                            .build();
                    paymentRawRepository.save(rawEntity);
                    acceptedRows++;
                }
            } catch (Exception e) {
                log.error("Error processing row {} in batch {}: {}", i + 1, batchId, e.getMessage());
                rejectedRows++;
                errors.add("Row " + (i + 1) + " processing failed: " + e.getMessage());
            }
        }
        
        return createSuccessResponse(batchId, acceptedRows, rejectedRows, warnings, errors, file, fileType);
    }
    
    private IngestionResponse processExcelRows(FileType fileType, String batchId, List<Row> rows, 
                                             List<String> warnings, List<String> errors, MultipartFile file) {
        int acceptedRows = 0;
        int rejectedRows = 0;
        
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            String rawData = convertRowToString(row);
            
            try {
                if (fileType == FileType.ORDERS) {
                    OrderRawEntity rawEntity = OrderRawEntity.builder()
                            .batchId(batchId)
                            .rowNumber(i + 1)
                            .rawData(rawData)
                            .validationStatus(OrderRawEntity.ValidationStatus.VALID)
                            .processed(false)
                            .build();
                    orderRawRepository.save(rawEntity);
                    acceptedRows++;
                } else {
                    PaymentRawEntity rawEntity = PaymentRawEntity.builder()
                            .batchId(batchId)
                            .rowNumber(i + 1)
                            .rawData(rawData)
                            .validationStatus(PaymentRawEntity.ValidationStatus.VALID)
                            .processed(false)
                            .build();
                    paymentRawRepository.save(rawEntity);
                    acceptedRows++;
                }
            } catch (Exception e) {
                log.error("Error processing Excel row {} in batch {}: {}", i + 1, batchId, e.getMessage());
                rejectedRows++;
                errors.add("Row " + (i + 1) + " processing failed: " + e.getMessage());
            }
        }
        
        return createSuccessResponse(batchId, acceptedRows, rejectedRows, warnings, errors, file, fileType);
    }
    
    private String convertRowToString(Row row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null) {
                switch (cell.getCellType()) {
                    case STRING -> sb.append(cell.getStringCellValue());
                    case NUMERIC -> sb.append(cell.getNumericCellValue());
                    case BOOLEAN -> sb.append(cell.getBooleanCellValue());
                    default -> sb.append("");
                }
            }
            if (i < row.getLastCellNum() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
    
    private IngestionResponse createSuccessResponse(String batchId, int acceptedRows, int rejectedRows, 
                                                  List<String> warnings, List<String> errors, MultipartFile file, FileType fileType) {
        IngestionResponse response = IngestionResponse.builder()
                .batchId(batchId)
                .acceptedRows(acceptedRows)
                .rejectedRows(rejectedRows)
                .warningsCount(warnings.size())
                .ingestedAt(LocalDateTime.now())
                .warnings(warnings)
                .errors(errors)
                .build();
        
        // Publish event
        publishFileIngestedEvent(batchId, acceptedRows + rejectedRows, file, fileType);
        
        return response;
    }
    
    private IngestionResponse createRejectionResponse(String batchId, List<String> warnings, List<String> errors) {
        return IngestionResponse.builder()
                .batchId(batchId)
                .acceptedRows(0)
                .rejectedRows(0)
                .warningsCount(warnings.size())
                .ingestedAt(LocalDateTime.now())
                .warnings(warnings)
                .errors(errors)
                .build();
    }
    
    private void publishFileIngestedEvent(String batchId, int rowCount, MultipartFile file, FileType fileType) {
        FileIngestedEvent event = FileIngestedEvent.builder()
                .batchId(batchId)
                .fileType(fileType)
                .rowCount(rowCount)
                .timeframe(LocalDateTime.now())
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .ingestedAt(LocalDateTime.now())
                .build();
        
        eventPublisher.publishEvent(event);
        log.info("Published file.ingested event for batch: {}, rows: {}", batchId, rowCount);
    }
    
    private String generateBatchId(FileType fileType) {
        String prefix = fileType == FileType.ORDERS ? "ORD" : "PAY";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return prefix + "_" + timestamp + "_" + random;
    }
    
    private boolean isCsv(MultipartFile file) {
        String name = file.getOriginalFilename();
        String contentType = file.getContentType();
        return (name != null && name.toLowerCase().endsWith(".csv")) ||
               (contentType != null && (contentType.equalsIgnoreCase("text/csv") || 
                                       contentType.equalsIgnoreCase("application/csv")));
    }
}
