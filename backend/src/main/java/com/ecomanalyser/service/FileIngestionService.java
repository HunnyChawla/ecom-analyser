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
            Sheet sheet = wb.getSheetAt(0);
            
            // Get headers from first row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                errors.add("No header row found in Excel file");
                return createRejectionResponse(batchId, warnings, errors);
            }
            
            Set<String> actualColumns = new HashSet<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    actualColumns.add(cell.getStringCellValue());
                }
            }
            
            // Validate schema
            SchemaValidationResult validationResult = schemaValidationService.validateSchema(actualColumns, fileType);
            warnings.addAll(validationResult.getWarnings());
            errors.addAll(validationResult.getErrors());
            
            if (!validationResult.isValid()) {
                log.warn("Schema validation failed for batch {}: {}", batchId, errors);
                return createRejectionResponse(batchId, warnings, errors);
            }
            
            // Process rows
            List<Row> dataRows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    dataRows.add(row);
                }
            }
            
            return processExcelRows(fileType, batchId, dataRows, warnings, errors, file);
        }
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
