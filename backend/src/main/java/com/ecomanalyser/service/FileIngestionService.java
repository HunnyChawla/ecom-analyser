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
             CSVParser parser = CSVFormat.DEFAULT
                 .withFirstRecordAsHeader()
                 .withIgnoreHeaderCase()
                 .withTrim()
                 .withIgnoreEmptyLines()
                 .withEscape('\\')
                 .withQuote('"')
                 .parse(reader)) {
            
            // Validate schema
            Set<String> actualColumns = parser.getHeaderMap().keySet();
            log.info("CSV headers detected: {}", actualColumns);
            
            SchemaValidationResult validationResult = schemaValidationService.validateSchema(actualColumns, fileType);
            
            warnings.addAll(validationResult.getWarnings());
            errors.addAll(validationResult.getErrors());
            
            if (!validationResult.isValid()) {
                log.warn("Schema validation failed for batch {}: {}", batchId, errors);
                return createRejectionResponse(batchId, warnings, errors);
            }
            
            // Process rows
            List<CSVRecord> records = parser.getRecords();
            log.info("Found {} CSV records to process", records.size());
            
            // Validate that we have records
            if (records.isEmpty()) {
                warnings.add("No data rows found in CSV file");
                return createSuccessResponse(batchId, 0, 0, warnings, errors, file, fileType);
            }
            
            // Log first few records for debugging
            for (int i = 0; i < Math.min(3, records.size()); i++) {
                CSVRecord record = records.get(i);
                log.debug("Sample record {}: size={}, values={}", i + 1, record.size(), 
                         Arrays.toString(record.values()));
                
                // Also log the raw record for debugging
                log.debug("Sample record {} raw: {}", i + 1, record);
                
                // Test the conversion methods
                String testDirect = convertCsvRecordToStringDirect(record);
                String testAlternative = convertCsvRecordToStringAlternative(record);
                String testRobust = convertCsvRecordToStringRobust(record);
                
                log.debug("Sample record {} conversion test - Direct: '{}', Alternative: '{}', Robust: '{}'", 
                         i + 1, testDirect, testAlternative, testRobust);
                
                // Validate that none of the methods return object references
                if (testDirect.contains("CSVRecord") || testAlternative.contains("CSVRecord") || testRobust.contains("CSVRecord")) {
                    log.error("CRITICAL: One or more conversion methods returned object references!");
                    log.error("Direct: '{}'", testDirect);
                    log.error("Alternative: '{}'", testAlternative);
                    log.error("Robust: '{}'", testRobust);
                }
            }
            
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
        
        log.info("Processing {} CSV records for batch {}", records.size(), batchId);
        
        for (int i = 0; i < records.size(); i++) {
            CSVRecord record = records.get(i);
            
            // Test CSVRecord behavior for the first few records
            if (i < 2) {
                testCsvRecordBehavior(record);
            }
            
            // Debug logging to see the CSVRecord object
            log.debug("Processing CSVRecord {}: {}", i + 1, record);
            log.debug("CSVRecord class: {}", record.getClass().getName());
            log.debug("CSVRecord size: {}", record.size());
            log.debug("CSVRecord recordNumber: {}", record.getRecordNumber());
            
            // Test different ways to access the data
            try {
                String[] directValues = record.values();
                log.debug("Direct values array: {}", Arrays.toString(directValues));
                
                // Try to access individual fields
                for (int j = 0; j < Math.min(5, record.size()); j++) {
                    try {
                        String fieldValue = record.get(j);
                        log.debug("Field {}: '{}'", j, fieldValue);
                    } catch (Exception e) {
                        log.error("Error accessing field {}: {}", j, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error accessing CSVRecord data: {}", e.getMessage());
            }
            
            // Convert CSV record to comma-separated string of actual values
            // Use the most reliable method first
            String rawData = convertCsvRecordToStringRobust(record);
            
            // Debug logging to see the converted result
            log.debug("Converted raw data for row {}: '{}'", i + 1, rawData);
            
            // Validate that the result doesn't contain object references
            if (rawData.contains("CSVRecord") || rawData.contains("recordNumber=") || rawData.contains("values=")) {
                log.error("CRITICAL ERROR: CSVRecord object reference found in raw data: '{}'", rawData);
                errors.add("Row " + (i + 1) + " contains object reference instead of data");
                rejectedRows++;
                continue;
            }
            
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
                    log.debug("Saved OrderRawEntity for row {} with rawData: '{}'", i + 1, rawData);
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
                    log.debug("Saved PaymentRawEntity for row {} with rawData: '{}'", i + 1, rawData);
                }
            } catch (Exception e) {
                log.error("Error processing row {} in batch {}: {}", i + 1, batchId, e.getMessage());
                rejectedRows++;
                errors.add("Row " + (i + 1) + " processing failed: " + e.getMessage());
            }
        }
        
        log.info("Completed processing CSV records. Accepted: {}, Rejected: {}", acceptedRows, rejectedRows);
        
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

    private String convertCsvRecordToString(CSVRecord record) {
        StringBuilder sb = new StringBuilder();
        
        // Debug logging to see what we're working with
        log.debug("Converting CSVRecord: size={}, recordNumber={}", record.size(), record.getRecordNumber());
        
        // Try to get the header names to use as a fallback
        Map<String, Integer> headerMap = record.getParser().getHeaderMap();
        log.debug("Header map: {}", headerMap);
        
        for (int i = 0; i < record.size(); i++) {
            String value = record.get(i);
            
            // Debug logging for each field
            log.debug("Field {}: '{}'", i, value);
            
            if (value != null) {
                // Trim the value to remove any leading/trailing whitespace
                String trimmedValue = value.trim();
                sb.append(trimmedValue);
            } else {
                // For null values, append empty string
                sb.append("");
            }
            
            // Add comma separator (but not after the last field)
            if (i < record.size() - 1) {
                sb.append(",");
            }
        }
        
        String result = sb.toString();
        log.debug("Converted CSVRecord to string: '{}'", result);
        
        return result;
    }
    
    /**
     * Alternative method to convert CSVRecord to string using header names
     * This can be more reliable in some cases
     */
    private String convertCsvRecordToStringAlternative(CSVRecord record) {
        StringBuilder sb = new StringBuilder();
        
        try {
            // Get the header map from the parser
            Map<String, Integer> headerMap = record.getParser().getHeaderMap();
            
            if (headerMap != null && !headerMap.isEmpty()) {
                // Use header names to extract values
                String[] headerNames = headerMap.keySet().toArray(new String[0]);
                
                for (int i = 0; i < headerNames.length; i++) {
                    String headerName = headerNames[i];
                    String value = record.get(headerName);
                    
                    log.debug("Header '{}': value='{}'", headerName, value);
                    
                    if (value != null) {
                        String trimmedValue = value.trim();
                        sb.append(trimmedValue);
                    } else {
                        sb.append("");
                    }
                    
                    if (i < headerNames.length - 1) {
                        sb.append(",");
                    }
                }
            } else {
                // Fallback to index-based access
                log.warn("No header map available, falling back to index-based access");
                return convertCsvRecordToString(record);
            }
        } catch (Exception e) {
            log.error("Error in alternative CSV conversion method: {}", e.getMessage());
            // Fallback to the original method
            return convertCsvRecordToString(record);
        }
        
        String result = sb.toString();
        log.debug("Alternative conversion result: '{}'", result);
        
        return result;
    }
    
    /**
     * Direct method using CSVRecord.values() array
     * This should be the most reliable method
     */
    private String convertCsvRecordToStringDirect(CSVRecord record) {
        try {
            // Get the values array directly from the CSVRecord
            String[] values = record.values();
            log.debug("Direct values array: {}", Arrays.toString(values));
            
            // Join the values with commas
            String result = String.join(",", values);
            log.debug("Direct conversion result: '{}'", result);
            
            return result;
        } catch (Exception e) {
            log.error("Error in direct CSV conversion method: {}", e.getMessage());
            // Fallback to the original method
            return convertCsvRecordToString(record);
        }
    }
    
    /**
     * Robust method that handles various CSV format issues
     */
    private String convertCsvRecordToStringRobust(CSVRecord record) {
        try {
            // First try the direct method
            String directResult = convertCsvRecordToStringDirect(record);
            
            // Validate the result - it should not contain "CSVRecord" or other object references
            if (directResult.contains("CSVRecord") || directResult.contains("recordNumber=") || directResult.contains("values=")) {
                log.warn("Direct method returned object reference, trying alternative method");
                
                // Try the header-based method
                String headerResult = convertCsvRecordToStringAlternative(record);
                
                // Validate this result too
                if (headerResult.contains("CSVRecord") || headerResult.contains("recordNumber=") || headerResult.contains("values=")) {
                    log.error("All methods failed, CSVRecord object is being converted to string incorrectly");
                    
                    // As a last resort, try to manually extract values
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < record.size(); i++) {
                        try {
                            String value = record.get(i);
                            if (value != null) {
                                sb.append(value.trim());
                            }
                            if (i < record.size() - 1) {
                                sb.append(",");
                            }
                        } catch (Exception e) {
                            log.error("Error extracting field {}: {}", i, e.getMessage());
                            sb.append("");
                            if (i < record.size() - 1) {
                                sb.append(",");
                            }
                        }
                    }
                    return sb.toString();
                }
                
                return headerResult;
            }
            
            return directResult;
            
        } catch (Exception e) {
            log.error("Error in robust CSV conversion method: {}", e.getMessage());
            
            // Try one more approach - use reflection to access the internal values
            try {
                log.warn("Attempting reflection-based approach to extract CSV values");
                return extractCsvValuesWithReflection(record);
            } catch (Exception reflectionException) {
                log.error("Reflection approach also failed: {}", reflectionException.getMessage());
                return "ERROR_PARSING_CSV";
            }
        }
    }
    
    /**
     * Last resort method using reflection to access CSVRecord internals
     */
    private String extractCsvValuesWithReflection(CSVRecord record) throws Exception {
        try {
            // Try to access the internal values field using reflection
            java.lang.reflect.Field valuesField = record.getClass().getDeclaredField("values");
            valuesField.setAccessible(true);
            String[] values = (String[]) valuesField.get(record);
            
            if (values != null) {
                log.info("Successfully extracted values using reflection: {}", Arrays.toString(values));
                return String.join(",", values);
            } else {
                log.warn("Reflection returned null values");
                return "ERROR_NO_VALUES";
            }
        } catch (Exception e) {
            log.error("Reflection failed: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Test method to understand CSVRecord behavior
     */
    private void testCsvRecordBehavior(CSVRecord record) {
        log.info("=== CSVRecord Behavior Test ===");
        log.info("Record class: {}", record.getClass().getName());
        log.info("Record size: {}", record.size());
        log.info("Record recordNumber: {}", record.getRecordNumber());
        
        // Test toString method
        String toStringResult = record.toString();
        log.info("toString() result: '{}'", toStringResult);
        
        // Test values method
        try {
            String[] values = record.values();
            log.info("values() result: {}", Arrays.toString(values));
        } catch (Exception e) {
            log.error("values() method failed: {}", e.getMessage());
        }
        
        // Test get method
        try {
            for (int i = 0; i < Math.min(3, record.size()); i++) {
                String value = record.get(i);
                log.info("get({}) result: '{}'", i, value);
            }
        } catch (Exception e) {
            log.error("get() method failed: {}", e.getMessage());
        }
        
        // Test if we can access the parser
        try {
            var parser = record.getParser();
            var headerMap = parser.getHeaderMap();
            log.info("Header map: {}", headerMap);
        } catch (Exception e) {
            log.error("Parser access failed: {}", e.getMessage());
        }
        
        log.info("=== End CSVRecord Behavior Test ===");
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
