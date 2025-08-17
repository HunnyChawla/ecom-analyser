package com.ecomanalyser.config;

import com.ecomanalyser.domain.NormalizedOrderEntity;
import com.ecomanalyser.domain.OrderRawEntity;
import com.ecomanalyser.domain.PaymentEntity;
import com.ecomanalyser.domain.PaymentRawEntity;
import com.ecomanalyser.domain.NormalizedPaymentEntity;
import com.ecomanalyser.service.SkuResolverService;
import com.ecomanalyser.service.StatusNormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {
    
    private final DataSource dataSource;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SkuResolverService skuResolverService;
    private final StatusNormalizationService statusNormalizationService;
    
    @Bean
    @org.springframework.context.annotation.Primary
    public Job normalizeRawOrdersJob() {
        return new JobBuilder("normalizeRawOrdersJob", jobRepository)
                .start(normalizeRawOrdersStep())
                .build();
    }

    @Bean
    public Job normalizeRawPaymentsJob() {
        return new JobBuilder("normalizeRawPaymentsJob", jobRepository)
                .start(normalizeRawPaymentsStep())
                .build();
    }
    
    @Bean
    public Step normalizeRawOrdersStep() {
        return new StepBuilder("normalizeRawOrdersStep", jobRepository)
                .<OrderRawEntity, NormalizedOrderEntity>chunk(100, transactionManager)
                .reader(orderRawItemReader())
                .processor(orderRawItemProcessor())
                .writer(normalizedOrderItemWriter())
                .faultTolerant()
                .skipLimit(100) // Increased from 10 to 100
                .skip(Exception.class)
                .listener(new NormalizationStepListener())
                .build();
    }

    @Bean
    public Step normalizeRawPaymentsStep() {
        return new StepBuilder("normalizeRawPaymentsStep", jobRepository)
                .<PaymentRawEntity, NormalizedPaymentEntity>chunk(100, transactionManager)
                .reader(paymentRawItemReader())
                .processor(paymentRawItemProcessor())
                .writer(normalizedPaymentItemWriter())
                .faultTolerant()
                .skipLimit(100)
                .skip(Exception.class)
                .listener(new NormalizationStepListener())
                .build();
    }
    
    @Bean
    public ItemReader<OrderRawEntity> orderRawItemReader() {
        return new JdbcCursorItemReaderBuilder<OrderRawEntity>()
                .name("orderRawItemReader")
                .dataSource(dataSource)
                .sql("SELECT id, batch_id, row_number, raw_data, validation_status, validation_errors, processed, created_at FROM orders_raw WHERE validation_status = 'VALID' AND processed = false ORDER BY id")
                .rowMapper(new DataClassRowMapper<>(OrderRawEntity.class))
                .build();
    }

    @Bean
    public ItemReader<PaymentRawEntity> paymentRawItemReader() {
        return new JdbcCursorItemReaderBuilder<PaymentRawEntity>()
                .name("paymentRawItemReader")
                .dataSource(dataSource)
                .sql("SELECT id, batch_id, row_number, raw_data, validation_status, validation_errors, processed, created_at FROM payments_raw WHERE validation_status = 'VALID' AND processed = false ORDER BY id")
                .rowMapper(new DataClassRowMapper<>(PaymentRawEntity.class))
                .build();
    }
    
    @Bean
    public ItemProcessor<OrderRawEntity, NormalizedOrderEntity> orderRawItemProcessor() {
        return new OrderRawItemProcessor(skuResolverService, statusNormalizationService);
    }

    @Bean
    public ItemProcessor<PaymentRawEntity, NormalizedPaymentEntity> paymentRawItemProcessor() {
        return new PaymentRawItemProcessor(statusNormalizationService);
    }
    
    @Bean
    public ItemWriter<NormalizedOrderEntity> normalizedOrderItemWriter() {
        return new JdbcBatchItemWriterBuilder<NormalizedOrderEntity>()
                .dataSource(dataSource)
                .sql("INSERT INTO normalized_orders (order_id, sku, quantity, selling_price, order_date, product_name, customer_state, size, supplier_listed_price, supplier_discounted_price, packet_id, standardized_status, original_status, supplier_sku, sku_resolved, validation_errors, batch_id, raw_row_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setString(1, item.getOrderId());
                    ps.setString(2, item.getSku());
                    ps.setObject(3, item.getQuantity());
                    ps.setObject(4, item.getSellingPrice());
                    ps.setObject(5, item.getOrderDate());
                    ps.setString(6, item.getProductName());
                    ps.setString(7, item.getCustomerState());
                    ps.setString(8, item.getSize());
                    ps.setObject(9, item.getSupplierListedPrice());
                    ps.setObject(10, item.getSupplierDiscountedPrice());
                    ps.setString(11, item.getPacketId());
                    ps.setString(12, item.getStandardizedStatus());
                    ps.setString(13, item.getOriginalStatus());
                    ps.setString(14, item.getSupplierSku());
                    ps.setBoolean(15, item.getSkuResolved());
                    ps.setString(16, item.getValidationErrors());
                    ps.setString(17, item.getBatchId());
                    ps.setObject(18, item.getRawRowId());
                })
                .build();
    }

    @Bean
    public ItemWriter<NormalizedPaymentEntity> normalizedPaymentItemWriter() {
        return new JdbcBatchItemWriterBuilder<NormalizedPaymentEntity>()
                .dataSource(dataSource)
                .sql("INSERT INTO normalized_payments (payment_id, order_id, amount, payment_date, standardized_status, original_status, transaction_id, price_type, validation_errors, batch_id, raw_row_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (order_id) DO UPDATE SET payment_id = EXCLUDED.payment_id, amount = EXCLUDED.amount, payment_date = EXCLUDED.payment_date, standardized_status = EXCLUDED.standardized_status, original_status = EXCLUDED.original_status, transaction_id = EXCLUDED.transaction_id, price_type = EXCLUDED.price_type, validation_errors = EXCLUDED.validation_errors, batch_id = EXCLUDED.batch_id, raw_row_id = EXCLUDED.raw_row_id")
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setString(1, item.getPaymentId());
                    ps.setString(2, item.getOrderId());
                    ps.setObject(3, item.getAmount());
                    ps.setObject(4, item.getPaymentDate());
                    ps.setString(5, item.getStandardizedStatus());
                    ps.setString(6, item.getOriginalStatus());
                    ps.setString(7, item.getTransactionId());
                    ps.setString(8, item.getPriceType());
                    ps.setString(9, item.getValidationErrors());
                    ps.setString(10, item.getBatchId());
                    ps.setObject(11, item.getRawRowId());
                })
                .build();
    }
    
    // Custom processor for transforming raw orders to normalized orders
    @RequiredArgsConstructor
    public static class OrderRawItemProcessor implements ItemProcessor<OrderRawEntity, NormalizedOrderEntity> {
        
        private final SkuResolverService skuResolverService;
        private final StatusNormalizationService statusNormalizationService;
        
        @Override
        public NormalizedOrderEntity process(OrderRawEntity rawOrder) throws Exception {
            try {
                // Parse raw data (assuming CSV format)
                String[] fields = parseRawData(rawOrder.getRawData());
                
                log.debug("Processing raw order row {} with {} fields: {}", rawOrder.getRowNumber(), fields.length, String.join("|", fields));
                
                if (fields.length < 11) {
                    log.warn("Invalid raw data format for row {}: insufficient fields", rawOrder.getRowNumber());
                    return null; // Skip invalid rows
                }
                
                // Extract fields from raw data
                String originalStatus = fields[0];
                String orderId = fields[1];
                String orderDateStr = fields[2];
                String customerState = fields[3];
                String productName = fields[4];
                String sku = fields[5];
                String size = fields[6];
                String quantityStr = fields[7];
                String supplierListedPriceStr = fields[8];
                String supplierDiscountedPriceStr = fields[9];
                String packetId = fields[10];
                
                log.debug("Extracted fields for row {}: status={}, orderId={}, date={}, state={}, product={}, sku={}, size={}, qty={}, listedPrice={}, discountedPrice={}, packetId={}", 
                         rawOrder.getRowNumber(), originalStatus, orderId, orderDateStr, customerState, productName, sku, size, quantityStr, supplierListedPriceStr, supplierDiscountedPriceStr, packetId);
                
                // Validate required fields
                if (orderId == null || orderId.trim().isEmpty()) {
                    log.warn("Missing order_id in row {}, skipping", rawOrder.getRowNumber());
                    return null;
                }
                
                // Process and normalize data
                NormalizedOrderEntity normalizedOrder = NormalizedOrderEntity.builder()
                        .orderId(orderId.trim())
                        .sku(resolveSku(sku, null)) // For now, no supplier_sku in orders
                        .quantity(parseInteger(quantityStr))
                        .sellingPrice(parseBigDecimal(supplierDiscountedPriceStr))
                        .orderDate(parseDate(orderDateStr))
                        .productName(productName)
                        .customerState(customerState)
                        .size(size)
                        .supplierListedPrice(parseBigDecimal(supplierListedPriceStr))
                        .supplierDiscountedPrice(parseBigDecimal(supplierDiscountedPriceStr))
                        .packetId(packetId)
                        .standardizedStatus(statusNormalizationService.normalizeStatus(originalStatus))
                        .originalStatus(originalStatus)
                        .supplierSku(null) // Not available in orders data
                        .skuResolved(sku != null && !sku.trim().isEmpty())
                        .validationErrors(null)
                        .batchId(rawOrder.getBatchId())
                        .rawRowId(rawOrder.getId())
                        .build();
                
                log.debug("Processed raw order {} to normalized order {}", rawOrder.getRowNumber(), normalizedOrder.getOrderId());
                return normalizedOrder;
                
            } catch (Exception e) {
                log.error("Error processing raw order row {}: {}", rawOrder.getRowNumber(), e.getMessage());
                return null; // Skip rows with processing errors
            }
        }
        
        private String[] parseRawData(String rawData) {
            if (rawData == null || rawData.trim().isEmpty()) {
                return new String[0];
            }
            
            // Simple CSV parsing that handles the actual data format
            // The rawData is already a comma-separated string from the ingestion process
            String[] fields = rawData.split(",");
            
            // Trim whitespace from each field
            for (int i = 0; i < fields.length; i++) {
                if (fields[i] != null) {
                    fields[i] = fields[i].trim();
                }
            }
            
            return fields;
        }
        
        private String resolveSku(String sku, String supplierSku) {
            return skuResolverService.resolveSku(sku, supplierSku);
        }
        
        private Integer parseInteger(String value) {
            try {
                return value != null && !value.trim().isEmpty() ? Integer.parseInt(value.trim()) : null;
            } catch (NumberFormatException e) {
                log.warn("Could not parse integer: {}", value);
                return null;
            }
        }
        
        private java.math.BigDecimal parseBigDecimal(String value) {
            try {
                return value != null && !value.trim().isEmpty() ? new java.math.BigDecimal(value.trim()) : null;
            } catch (NumberFormatException e) {
                log.warn("Could not parse decimal: {}", value);
                return null;
            }
        }
        
        private LocalDate parseDate(String dateStr) {
            try {
                if (dateStr == null || dateStr.trim().isEmpty()) {
                    return null;
                }
                
                // Try different date formats
                String[] formats = {"yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy"};
                for (String format : formats) {
                    try {
                        return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern(format));
                    } catch (Exception ignored) {
                        // Try next format
                    }
                }
                
                log.warn("Could not parse date: {}", dateStr);
                return null;
                
            } catch (Exception e) {
                log.warn("Error parsing date: {}", dateStr);
                return null;
            }
        }
    }

    // Processor for transforming raw payments to normalized payments (payments table)
    @RequiredArgsConstructor
    public static class PaymentRawItemProcessor implements ItemProcessor<PaymentRawEntity, NormalizedPaymentEntity> {

        private final StatusNormalizationService statusNormalizationService;

        @Override
        public NormalizedPaymentEntity process(PaymentRawEntity rawPayment) {
            try {
                String[] fields = parseRawData(rawPayment.getRawData());

                if (fields.length < 12) { // need at least sub order no, status, transaction id, payment date, amount
                    log.warn("Skipping payment row {}: insufficient fields (found {})", rawPayment.getRowNumber(), fields.length);
                    return null;
                }

                String orderId = safeGet(fields, 0);
                String originalStatus = safeGet(fields, 5);
                String transactionId = safeGet(fields, 9);
                String paymentDateStr = safeGet(fields, 10);
                String amountStr = safeGet(fields, 11);
                String priceType = safeGet(fields, 12);

                if (orderId == null || orderId.isBlank()) {
                    log.warn("Skipping payment row {}: missing orderId", rawPayment.getRowNumber());
                    return null;
                }

                // Clean/validate amount and currency
                String detectedCurrency = detectCurrency(amountStr);
                if (detectedCurrency != null && !detectedCurrency.equalsIgnoreCase("INR")) {
                    log.warn("Payment row {}: non-INR currency detected '{}' in amount '{}'; proceeding after conversion attempt", rawPayment.getRowNumber(), detectedCurrency, amountStr);
                }

                java.math.BigDecimal amount = parseAmount(amountStr);
                if (amount == null) {
                    log.warn("Skipping payment row {}: invalid amount '{}'", rawPayment.getRowNumber(), amountStr);
                    return null; // invalid amount -> skip
                }

                java.time.LocalDate paymentDate = parseDate(paymentDateStr);

                return NormalizedPaymentEntity.builder()
                        .paymentId(transactionId != null && !transactionId.isBlank() ? transactionId : orderId)
                        .orderId(orderId.trim())
                        .amount(amount)
                        .paymentDate(paymentDate)
                        .standardizedStatus(statusNormalizationService.normalizeStatus(originalStatus))
                        .originalStatus(originalStatus)
                        .transactionId(transactionId)
                        .priceType(priceType)
                        .validationErrors(null)
                        .batchId(rawPayment.getBatchId())
                        .rawRowId(rawPayment.getId())
                        .build();

            } catch (Exception e) {
                return null;
            }
        }

        private static String[] parseRawData(String rawData) {
            if (rawData == null || rawData.trim().isEmpty()) return new String[0];
            String[] fields = rawData.split(",");
            for (int i = 0; i < fields.length; i++) {
                if (fields[i] != null) fields[i] = fields[i].trim();
            }
            return fields;
        }

        private static String safeGet(String[] arr, int idx) {
            return idx >= 0 && idx < arr.length ? arr[idx] : null;
        }

        private static java.math.BigDecimal parseAmount(String value) {
            if (value == null || value.trim().isEmpty()) return null;
            String cleaned = value.replaceAll("[₹$,]", "").replaceAll("\\s*(INR|USD|EUR)\\s*", "");
            try {
                return new java.math.BigDecimal(cleaned.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private static String detectCurrency(String value) {
            if (value == null) return null;
            String v = value.toUpperCase();
            if (v.contains("INR") || v.contains("₹")) return "INR";
            if (v.contains("USD") || v.contains("$")) return "USD";
            if (v.contains("EUR") || v.contains("€")) return "EUR";
            return null;
        }

        private static java.time.LocalDate parseDate(String dateStr) {
            try {
                if (dateStr == null || dateStr.trim().isEmpty()) return null;
                String[] formats = {"yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy"};
                for (String f : formats) {
                    try {
                        return java.time.LocalDate.parse(dateStr.trim(), java.time.format.DateTimeFormatter.ofPattern(f));
                    } catch (Exception ignored) {}
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }
    }
    
    // Step listener for monitoring and logging
    public static class NormalizationStepListener implements org.springframework.batch.core.StepExecutionListener {
        
        @Override
        public void beforeStep(org.springframework.batch.core.StepExecution stepExecution) {
            log.info("Starting normalization step: {}", stepExecution.getStepName());
        }
        
        @Override
        public org.springframework.batch.core.ExitStatus afterStep(org.springframework.batch.core.StepExecution stepExecution) {
            log.info("Completed normalization step: {} - Read: {}, Written: {}, Skipped: {}", 
                    stepExecution.getStepName(),
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount(),
                    stepExecution.getSkipCount());
            return stepExecution.getExitStatus();
        }
    }
}
