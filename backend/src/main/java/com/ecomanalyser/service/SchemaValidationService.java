package com.ecomanalyser.service;

import com.ecomanalyser.event.FileIngestedEvent.FileType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class SchemaValidationService {
    
    // Expected columns for Orders
    private static final Set<String> EXPECTED_ORDER_COLUMNS = Set.of(
        "reason for credit entry", "sub order no", "order date", "customer state",
        "product name", "sku", "size", "quantity", "supplier listed price (incl. gst + commission)",
        "supplier discounted price (incl gst and commision)", "packet id"
    );
    
    // Expected columns for Payments
    private static final Set<String> EXPECTED_PAYMENT_COLUMNS = Set.of(
        "sub order no", "order date", "dispatch date", "product name", "supplier sku",
        "live order status", "product gst %", "listing price (incl. taxes)", "quantity",
        "transaction id", "payment date", "final settlement amount", "price type",
        "total sale amount (incl. shipping & gst)", "total sale return amount (incl. shipping & gst)",
        "fixed fee (incl. gst)", "warehousing fee (incl. gst)", "return premium (incl. gst)",
        "return premium (incl. gst) of return", "meesho commission percentage",
        "meesho commission (incl. gst)", "meesho gold platform fee (incl. gst)",
        "meesho mall platform fee (incl. gst)", "fixed fee (incl. gst)",
        "warehousing fee (incl. gst)", "return shipping charge (incl. gst)",
        "gst compensation (prp shipping)", "shipping charge (incl. gst)",
        "other support service charges (excl. gst)", "waivers (excl. gst)",
        "net other support service charges (excl. gst)", "gst on net other support service charges",
        "tcs", "tds rate %", "tds", "compensation", "compensation reason", "claims",
        "claims reason", "recovery", "recovery reason"
    );
    
    public SchemaValidationResult validateSchema(Set<String> actualColumns, FileType fileType) {
        Set<String> expectedColumns = getExpectedColumns(fileType);
        Set<String> unknownColumns = new HashSet<>();
        Set<String> missingColumns = new HashSet<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        // Check for unknown columns
        for (String column : actualColumns) {
            if (!expectedColumns.contains(column.toLowerCase())) {
                unknownColumns.add(column);
                warnings.add("Unknown column detected: " + column);
            }
        }
        
        // Check for missing critical columns
        Set<String> criticalColumns = getCriticalColumns(fileType);
        for (String criticalColumn : criticalColumns) {
            if (!actualColumns.stream().anyMatch(col -> col.toLowerCase().equals(criticalColumn))) {
                missingColumns.add(criticalColumn);
                errors.add("Missing critical column: " + criticalColumn);
            }
        }
        
        boolean isValid = errors.isEmpty();
        
        return SchemaValidationResult.builder()
                .valid(isValid)
                .unknownColumns(unknownColumns)
                .missingColumns(missingColumns)
                .warnings(warnings)
                .errors(errors)
                .build();
    }
    
    private Set<String> getExpectedColumns(FileType fileType) {
        return switch (fileType) {
            case ORDERS -> EXPECTED_ORDER_COLUMNS;
            case PAYMENTS -> EXPECTED_PAYMENT_COLUMNS;
        };
    }
    
    private Set<String> getCriticalColumns(FileType fileType) {
        return switch (fileType) {
            case ORDERS -> Set.of("sub order no", "sku", "quantity", "order date");
            case PAYMENTS -> Set.of("sub order no", "live order status", "final settlement amount");
        };
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SchemaValidationResult {
        private boolean valid;
        private Set<String> unknownColumns;
        private Set<String> missingColumns;
        private List<String> warnings;
        private List<String> errors;
    }
}
