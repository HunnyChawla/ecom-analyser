package com.ecomanalyser.service;

import com.ecomanalyser.domain.OrderEntity;
import com.ecomanalyser.domain.PaymentEntity;
import com.ecomanalyser.domain.SkuPriceEntity;
import com.ecomanalyser.repository.OrderRepository;
import com.ecomanalyser.repository.PaymentRepository;
import com.ecomanalyser.repository.SkuPriceRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelImportService.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SkuPriceRepository skuPriceRepository;

    // Collect per-request import warnings (thread-local for web requests)
    private final ThreadLocal<java.util.List<String>> importWarnings = ThreadLocal.withInitial(java.util.ArrayList::new);

    private void warn(String message) {
        importWarnings.get().add(message);
    }

    public java.util.List<String> consumeWarnings() {
        var list = new java.util.ArrayList<>(importWarnings.get());
        importWarnings.get().clear();
        return list;
    }

    // Truncate overly long strings to fit VARCHAR(255)
    private String clamp(String value, String fieldName) {
        if (value == null) return null;
        String v = value.trim();
        if (v.length() > 255) {
            warn(fieldName + " length " + v.length() + " > 255; truncated");
            return v.substring(0, 255);
        }
        return v;
    }

    @Transactional
    public int importOrders(MultipartFile file) throws Exception {
        log.info("Starting order import for file: {}", file.getOriginalFilename());
        if (isCsv(file)) {
            log.info("Detected CSV file, using CSV parser");
            return importOrdersCsv(file);
        } else {
            log.info("Detected Excel file, using Excel parser");
            // Reset warnings for this run
            importWarnings.get().clear();
            List<OrderEntity> toSave = new ArrayList<>();
            try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
                Sheet sheet = wb.getSheetAt(0);
                log.info("Excel sheet: {}, total rows: {}", sheet.getSheetName(), sheet.getPhysicalNumberOfRows());
                if (sheet.getPhysicalNumberOfRows() == 0) return 0;

                Row header = findHeaderRow(sheet,
                        List.of("Sub Order No"),
                        List.of("Quantity"),
                        List.of("Order Date"));
                log.info("Detected header row at index: {}", header.getRowNum());
                Map<String,Integer> hmap = buildHeaderIndex(header);
                log.info("Header index map: {}", hmap);

                int firstDataRow = header.getRowNum() + 1;
                for (int r = firstDataRow; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    
                    String orderId = getCellAny(row, hmap, List.of("Sub Order No", "Order Id", "Order ID", "Sub Order"), null);
                    if (orderId == null || orderId.isBlank()) {
                        orderId = "UNKNOWN-" + System.currentTimeMillis() + "-" + r;
                        warn("Row " + r + ": Missing orderId; generated " + orderId);
                    }
                    orderId = clamp(orderId, "order_id");
                    
                    String sku = getCellAny(row, hmap, List.of("SKU", "Supplier SKU", "Product SKU"), null);
                    if (sku == null || sku.isBlank()) {
                        sku = "UNKNOWN";
                        warn("Row " + r + " (" + orderId + "): Missing SKU; set to UNKNOWN");
                    }
                    
                    String qtyStr = getCellAny(row, hmap, List.of("Quantity", "Qty"), null);
                    int qty = parseIntFlexible(qtyStr);
                    if (qty <= 0) {
                        warn("Row " + r + " (" + orderId + "): Quantity invalid; set to 0");
                        qty = 0;
                    }
                    
                    String priceStr = getCellAny(row, hmap, List.of("Supplier Discounted Price (Incl GST and Commision)",
                            "Supplier Discounted Price (Incl GST and Commission)",
                            "Supplier Listed Price (Incl. GST + Commission)",
                            "Listing Price", "Unit Price", "Price"), null);
                    BigDecimal price = parseBigDecimal(priceStr);
                    if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                        warn("Row " + r + " (" + orderId + "): Selling price missing/invalid; set to 0");
                        price = BigDecimal.ZERO;
                    }
                    
                    String dateStr = getCellAny(row, hmap, List.of("Order Date", "Date", "OrderDate"), null);
                    LocalDate date = parseToLocalDate(dateStr);
                    if (date == null) {
                        warn("Row " + r + " (" + orderId + "): Order date missing/invalid; set to today");
                        date = LocalDate.now();
                    }
                    
                    // Get all additional fields
                    String productName = getCellAny(row, hmap, List.of("Product Name", "Product"), null);
                    String customerState = getCellAny(row, hmap, List.of("Customer State", "State"), null);
                    String size = getCellAny(row, hmap, List.of("Size"), null);
                    String supplierListedPriceStr = getCellAny(row, hmap, List.of("Supplier Listed Price (Incl. GST + Commission)"), null);
                    BigDecimal supplierListedPrice = parseBigDecimal(supplierListedPriceStr);
                    String supplierDiscountedPriceStr = getCellAny(row, hmap, List.of("Supplier Discounted Price (Incl GST and Commision)", "Supplier Discounted Price (Incl GST and Commission)"), null);
                    BigDecimal supplierDiscountedPrice = parseBigDecimal(supplierDiscountedPriceStr);
                    String packetId = getCellAny(row, hmap, List.of("Packet Id", "Packet ID"), null);
                    String reasonForCreditEntry = getCellAny(row, hmap, List.of("Reason for Credit Entry", "Credit Entry Reason"), null);
                    
                    toSave.add(OrderEntity.builder()
                            .orderId(orderId)
                            .sku(sku)
                            .quantity(qty)
                            .sellingPrice(price)
                            .orderDateTime(date.atStartOfDay())
                            .productName(productName)
                            .customerState(customerState)
                            .size(size)
                            .supplierListedPrice(supplierListedPrice)
                            .supplierDiscountedPrice(supplierDiscountedPrice)
                            .packetId(packetId)
                            .reasonForCreditEntry(reasonForCreditEntry)
                            .build());
                }
            }
            log.info("Total order entities to save: {}", toSave.size());
            
            // Handle duplicates by using upsert logic
            int savedCount = 0;
            for (OrderEntity order : toSave) {
                try {
                    // Check if order already exists
                    var existingOrder = orderRepository.findByOrderId(order.getOrderId());
                    if (existingOrder.isPresent()) {
                        log.info("Order {} already exists, updating...", order.getOrderId());
                        var existing = existingOrder.get();
                        // Update all fields
                        existing.setSku(order.getSku());
                        existing.setQuantity(order.getQuantity());
                        existing.setSellingPrice(order.getSellingPrice());
                        existing.setOrderDateTime(order.getOrderDateTime());
                        existing.setProductName(order.getProductName());
                        existing.setCustomerState(order.getCustomerState());
                        existing.setSize(order.getSize());
                        existing.setSupplierListedPrice(order.getSupplierListedPrice());
                        existing.setSupplierDiscountedPrice(order.getSupplierDiscountedPrice());
                        existing.setPacketId(order.getPacketId());
                        existing.setReasonForCreditEntry(order.getReasonForCreditEntry());
                        orderRepository.save(existing);
                        savedCount++;
                    } else {
                        // New order, save it
                        orderRepository.save(order);
                        savedCount++;
                    }
                } catch (Exception e) {
                    log.error("Error processing order {}: {}", order.getOrderId(), e.getMessage());
                    throw e;
                }
            }
            
            log.info("Successfully saved {} order entities", savedCount);
            return savedCount;
        }
    }

    @Transactional
    public int importPayments(MultipartFile file) throws Exception {
        log.info("Starting payment import for file: {}", file.getOriginalFilename());
        if (isCsv(file)) {
            log.info("Detected CSV file, using CSV parser");
            return importPaymentsCsv(file);
        } else {
            log.info("Detected Excel file, using Excel parser");
            // Reset warnings for this run
            importWarnings.get().clear();
            List<PaymentEntity> toSave = new ArrayList<>();
            try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
                // Look for the "Order Payments" sheet specifically
                Sheet sheet = null;
                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    String sheetName = wb.getSheetName(i);
                    log.info("Found sheet: '{}'", sheetName);
                    if ("Order Payments".equalsIgnoreCase(sheetName)) {
                        sheet = wb.getSheetAt(i);
                        log.info("Using sheet: '{}'", sheetName);
                        break;
                    }
                }
                
                if (sheet == null) {
                    log.warn("'Order Payments' sheet not found, falling back to first sheet");
                    sheet = wb.getSheetAt(0);
                }
                
                log.info("Excel sheet: {}, total rows: {}", sheet.getSheetName(), sheet.getPhysicalNumberOfRows());
                if (sheet.getPhysicalNumberOfRows() == 0) return 0;

                // Use a single header row approach - try row 1 (2nd row) as per your sheet description
                Row header = sheet.getRow(1);
                if (header == null) {
                    log.warn("Header row 1 not found, trying first row");
                    header = sheet.getRow(0);
                }
                
                if (header == null) {
                    log.error("No header row found");
                    return 0;
                }
                
                log.info("Using header row at index: {}", header.getRowNum());
                Map<String,Integer> hmap = buildHeaderIndex(header);
                log.info("Header index map: {}", hmap);
                
                int firstDataRow = header.getRowNum() + 2; // data starts 2 rows after header
                log.info("First data row: {}", firstDataRow);
                
                // Use getPhysicalNumberOfRows() for more reliable row counting
                int totalRows = sheet.getPhysicalNumberOfRows();
                int lastRowIndex = totalRows - 1; // Convert to 0-based index
                log.info("Total physical rows: {}, Last row index: {}, Will process rows {} to {}", 
                        totalRows, lastRowIndex, firstDataRow, lastRowIndex);
                
                int processedRows = 0;
                int skippedRows = 0;
                int validRows = 0;
                
                for (int r = firstDataRow; r <= lastRowIndex; r++) {
                    processedRows++;
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        skippedRows++;
                        log.debug("Row {}: Skipping null row", r);
                        continue;
                    }
                    
                    String orderId = getCellAny(row, hmap, List.of("Sub Order No", "Order Id", "Order ID", "Sub Order"), null);
                    if (orderId == null || orderId.isBlank()) {
                        orderId = "UNKNOWN-" + System.currentTimeMillis() + "-" + r;
                        warn("Row " + r + ": Missing orderId; generated " + orderId);
                    }
                    orderId = clamp(orderId, "order_id");
                    
                    String paymentId = clamp(getCellAny(row, hmap, List.of("Transaction ID", "Payment Id", "Payment ID", "Transaction"), null), "payment_id");
                    if (paymentId.isBlank()) {
                        paymentId = orderId + "-PAY"; // fallback payment ID
                    }
                    
                    String amtStr = getCellAny(row, hmap, List.of("Final Settlement Amount", "Net Settlement Amount", "Amount"), null);
                    String dateStr = getCellAny(row, hmap, List.of("Payment Date", "Settlement Date", "Date"), null);
                    
                    if ((amtStr == null || amtStr.isBlank()) && (dateStr == null || dateStr.isBlank())) {
                        skippedRows++;
                        log.debug("Row {}: Skipping row with blank amount and date", r);
                        continue; // likely an empty row
                    }
                    
                    BigDecimal amount = parseBigDecimal(amtStr);
                    LocalDate date = parseToLocalDate(dateStr);
                    if (amount == null) {
                        amount = BigDecimal.ZERO;
                        warn("Row " + r + " (" + orderId + "): Amount missing/invalid; set to 0");
                    }
                    if (date == null) {
                        date = LocalDate.now();
                        warn("Row " + r + " (" + orderId + "): Payment date missing/invalid; set to today");
                    }
                    
                    // Get order status from column F (Live Order Status)
                    String orderStatus = clamp(getCellAny(row, hmap, List.of("Live Order Status", "Order Status", "Status"), null), "order_status");
                    
                    // Do not skip for missing order status; default and warn
                    if (orderStatus == null || orderStatus.isBlank()) {
                        orderStatus = "UNKNOWN";
                        warn("Row " + r + " (" + orderId + "): Missing order status; set to UNKNOWN");
                    }
                    
                    validRows++;
                    log.debug("Row {}: Valid row - orderId={}, paymentId={}, amount={}, date={}, status={}", 
                             r, orderId, paymentId, amount, date, orderStatus);
                    
                    // Get all additional payment fields
                    String transactionId = clamp(getCellAny(row, hmap, List.of("Transaction ID", "Transaction Id"), null), "transaction_id");
                    String finalSettlementAmountStr = getCellAny(row, hmap, List.of("Final Settlement Amount", "Net Settlement Amount"), null);
                    BigDecimal finalSettlementAmount = parseBigDecimal(finalSettlementAmountStr);
                    String priceType = clamp(getCellAny(row, hmap, List.of("Price Type"), null), "price_type");
                    String totalSaleAmountStr = getCellAny(row, hmap, List.of("Total Sale Amount (Incl. Shipping & GST)"), null);
                    BigDecimal totalSaleAmount = parseBigDecimal(totalSaleAmountStr);
                    String totalSaleReturnAmountStr = getCellAny(row, hmap, List.of("Total Sale Return Amount (Incl. Shipping & GST)"), null);
                    BigDecimal totalSaleReturnAmount = parseBigDecimal(totalSaleReturnAmountStr);
                    String fixedFeeStr = getCellAny(row, hmap, List.of("Fixed Fee (Incl. GST)"), null);
                    BigDecimal fixedFee = parseBigDecimal(fixedFeeStr);
                    String warehousingFeeStr = getCellAny(row, hmap, List.of("Warehousing fee (Incl. GST)"), null);
                    BigDecimal warehousingFee = parseBigDecimal(warehousingFeeStr);
                    String returnPremiumStr = getCellAny(row, hmap, List.of("Return premium (Incl. GST)", "Return premium (Incl. GST) of Return"), null);
                    BigDecimal returnPremium = parseBigDecimal(returnPremiumStr);
                    String meeshoCommissionPercentageStr = getCellAny(row, hmap, List.of("Meesho Commission Percentage"), null);
                    BigDecimal meeshoCommissionPercentage = parseBigDecimal(meeshoCommissionPercentageStr);
                    String meeshoCommissionStr = getCellAny(row, hmap, List.of("Meesho Commission (Incl. GST)"), null);
                    BigDecimal meeshoCommission = parseBigDecimal(meeshoCommissionStr);
                    String meeshoGoldPlatformFeeStr = getCellAny(row, hmap, List.of("Meesho gold platform fee (Incl. GST)"), null);
                    BigDecimal meeshoGoldPlatformFee = parseBigDecimal(meeshoGoldPlatformFeeStr);
                    String meeshoMallPlatformFeeStr = getCellAny(row, hmap, List.of("Meesho mall platform fee (Incl. GST)"), null);
                    BigDecimal meeshoMallPlatformFee = parseBigDecimal(meeshoMallPlatformFeeStr);
                    String returnShippingChargeStr = getCellAny(row, hmap, List.of("Return Shipping Charge (Incl. GST)"), null);
                    BigDecimal returnShippingCharge = parseBigDecimal(returnShippingChargeStr);
                    String gstCompensationStr = getCellAny(row, hmap, List.of("GST Compensation (PRP Shipping)"), null);
                    BigDecimal gstCompensation = parseBigDecimal(gstCompensationStr);
                    String shippingChargeStr = getCellAny(row, hmap, List.of("Shipping Charge (Incl. GST)"), null);
                    BigDecimal shippingCharge = parseBigDecimal(shippingChargeStr);
                    String otherSupportServiceChargesStr = getCellAny(row, hmap, List.of("Other Support Service Charges (Excl. GST)"), null);
                    BigDecimal otherSupportServiceCharges = parseBigDecimal(otherSupportServiceChargesStr);
                    String waiversStr = getCellAny(row, hmap, List.of("Waivers (Excl. GST)"), null);
                    BigDecimal waivers = parseBigDecimal(waiversStr);
                    String netOtherSupportServiceChargesStr = getCellAny(row, hmap, List.of("Net Other Support Service Charges (Excl. GST)"), null);
                    BigDecimal netOtherSupportServiceCharges = parseBigDecimal(netOtherSupportServiceChargesStr);
                    String gstOnNetOtherSupportServiceChargesStr = getCellAny(row, hmap, List.of("GST on Net Other Support Service Charges"), null);
                    BigDecimal gstOnNetOtherSupportServiceCharges = parseBigDecimal(gstOnNetOtherSupportServiceChargesStr);
                    String tcsStr = getCellAny(row, hmap, List.of("TCS"), null);
                    BigDecimal tcs = parseBigDecimal(tcsStr);
                    String tdsRatePercentageStr = getCellAny(row, hmap, List.of("TDS Rate %"), null);
                    BigDecimal tdsRatePercentage = parseBigDecimal(tdsRatePercentageStr);
                    String tdsStr = getCellAny(row, hmap, List.of("TDS"), null);
                    BigDecimal tds = parseBigDecimal(tdsStr);
                    String compensationStr = getCellAny(row, hmap, List.of("Compensation"), null);
                    BigDecimal compensation = parseBigDecimal(compensationStr);
                    String claimsStr = getCellAny(row, hmap, List.of("Claims"), null);
                    BigDecimal claims = parseBigDecimal(claimsStr);
                    String recoveryStr = getCellAny(row, hmap, List.of("Recovery"), null);
                    BigDecimal recovery = parseBigDecimal(recoveryStr);
                    String compensationReason = clamp(getCellAny(row, hmap, List.of("Compensation Reason"), null), "compensation_reason");
                    String claimsReason = clamp(getCellAny(row, hmap, List.of("Claims Reason"), null), "claims_reason");
                    String recoveryReason = clamp(getCellAny(row, hmap, List.of("Recovery Reason"), null), "recovery_reason");
                    String dispatchDateStr = getCellAny(row, hmap, List.of("Dispatch Date"), null);
                    LocalDate dispatchDate = parseToLocalDate(dispatchDateStr);
                    String productGstPercentageStr = getCellAny(row, hmap, List.of("Product GST %"), null);
                    BigDecimal productGstPercentage = parseBigDecimal(productGstPercentageStr);
                    String listingPriceInclTaxesStr = getCellAny(row, hmap, List.of("Listing Price (Incl. taxes)"), null);
                    BigDecimal listingPriceInclTaxes = parseBigDecimal(listingPriceInclTaxesStr);
                    
                    // Enrich with order SKU and order date
                    String skuForOrder = null;
                    LocalDateTime orderDateTimeVal = null;
                    try {
                        var orderOpt = orderRepository.findByOrderId(orderId);
                        if (orderOpt.isPresent()) {
                            skuForOrder = clamp(orderOpt.get().getSku(), "sku");
                            orderDateTimeVal = orderOpt.get().getOrderDateTime();
                        }
                    } catch (Exception ignored) {}

                    toSave.add(PaymentEntity.builder()
                            .paymentId(paymentId)
                            .orderId(orderId)
                            .sku(skuForOrder)
                            .amount(amount)
                            .paymentDateTime(date != null ? date.atStartOfDay() : null)
                            .orderDateTime(orderDateTimeVal)
                            .orderStatus(orderStatus)
                            .transactionId(transactionId)
                            .finalSettlementAmount(finalSettlementAmount)
                            .priceType(priceType)
                            .totalSaleAmount(totalSaleAmount)
                            .totalSaleReturnAmount(totalSaleReturnAmount)
                            .fixedFee(fixedFee)
                            .warehousingFee(warehousingFee)
                            .returnPremium(returnPremium)
                            .meeshoCommissionPercentage(meeshoCommissionPercentage)
                            .meeshoCommission(meeshoCommission)
                            .meeshoGoldPlatformFee(meeshoGoldPlatformFee)
                            .meeshoMallPlatformFee(meeshoMallPlatformFee)
                            .returnShippingCharge(returnShippingCharge)
                            .gstCompensation(gstCompensation)
                            .shippingCharge(shippingCharge)
                            .otherSupportServiceCharges(otherSupportServiceCharges)
                            .waivers(waivers)
                            .netOtherSupportServiceCharges(netOtherSupportServiceCharges)
                            .gstOnNetOtherSupportServiceCharges(gstOnNetOtherSupportServiceCharges)
                            .tcs(tcs)
                            .tdsRatePercentage(tdsRatePercentage)
                            .tds(tds)
                            .compensation(compensation)
                            .claims(claims)
                            .recovery(recovery)
                            .compensationReason(compensationReason)
                            .claimsReason(claimsReason)
                            .recoveryReason(recoveryReason)
                            .dispatchDate(dispatchDate)
                            .productGstPercentage(productGstPercentage)
                            .listingPriceInclTaxes(listingPriceInclTaxes)
                            .build());
                }
                
                log.info("Row processing summary: Total processed={}, Valid rows={}, Skipped rows={}, Entities to save={}", 
                        processedRows, validRows, skippedRows, toSave.size());
                log.info("Total payment entities to process: {} (processed rows {} to {})", 
                        toSave.size(), firstDataRow, lastRowIndex);
                
                // Handle duplicates by using upsert logic based on order_id (Sub Order No)
                int savedCount = 0;
                for (PaymentEntity payment : toSave) {
                    try {
                        // Check if payment already exists based on order_id (Sub Order No)
                        var existingPayment = paymentRepository.findByOrderId(payment.getOrderId());
                        if (existingPayment.isPresent()) {
                            log.info("Payment for order {} already exists, updating...", payment.getOrderId());
                            var existing = existingPayment.get();
                            // Update all fields
                            existing.setPaymentId(payment.getPaymentId());
                            existing.setAmount(payment.getAmount());
                            existing.setPaymentDateTime(payment.getPaymentDateTime());
                            existing.setOrderDateTime(payment.getOrderDateTime());
                            existing.setOrderStatus(payment.getOrderStatus());
                            existing.setSku(payment.getSku());
                            existing.setTransactionId(payment.getTransactionId());
                            existing.setFinalSettlementAmount(payment.getFinalSettlementAmount());
                            existing.setPriceType(payment.getPriceType());
                            existing.setTotalSaleAmount(payment.getTotalSaleAmount());
                            existing.setTotalSaleReturnAmount(payment.getTotalSaleReturnAmount());
                            existing.setFixedFee(payment.getFixedFee());
                            existing.setWarehousingFee(payment.getWarehousingFee());
                            existing.setReturnPremium(payment.getReturnPremium());
                            existing.setMeeshoCommissionPercentage(payment.getMeeshoCommissionPercentage());
                            existing.setMeeshoCommission(payment.getMeeshoCommission());
                            existing.setMeeshoGoldPlatformFee(payment.getMeeshoGoldPlatformFee());
                            existing.setMeeshoMallPlatformFee(payment.getMeeshoMallPlatformFee());
                            existing.setReturnShippingCharge(payment.getReturnShippingCharge());
                            existing.setGstCompensation(payment.getGstCompensation());
                            existing.setShippingCharge(payment.getShippingCharge());
                            existing.setOtherSupportServiceCharges(payment.getOtherSupportServiceCharges());
                            existing.setWaivers(payment.getWaivers());
                            existing.setNetOtherSupportServiceCharges(payment.getNetOtherSupportServiceCharges());
                            existing.setGstOnNetOtherSupportServiceCharges(payment.getGstOnNetOtherSupportServiceCharges());
                            existing.setTcs(payment.getTcs());
                            existing.setTdsRatePercentage(payment.getTdsRatePercentage());
                            existing.setTds(payment.getTds());
                            existing.setCompensation(payment.getCompensation());
                            existing.setClaims(payment.getClaims());
                            existing.setRecovery(payment.getRecovery());
                            existing.setCompensationReason(payment.getCompensationReason());
                            existing.setClaimsReason(payment.getClaimsReason());
                            existing.setRecoveryReason(payment.getRecoveryReason());
                            existing.setDispatchDate(payment.getDispatchDate());
                            existing.setProductGstPercentage(payment.getProductGstPercentage());
                            existing.setListingPriceInclTaxes(payment.getListingPriceInclTaxes());
                            paymentRepository.save(existing);
                            savedCount++;
                        } else {
                            // New payment, save it
                            paymentRepository.save(payment);
                            savedCount++;
                        }
                    } catch (Exception e) {
                        log.error("Error processing payment for order {}: {}", payment.getOrderId(), e.getMessage());
                        throw e;
                    }
                }
                
                log.info("Successfully processed {} payment entities", savedCount);
                return savedCount;
            }
        }
    }

    @Transactional
    public int importSkuPrices(MultipartFile file) throws Exception {
        if (isCsv(file)) {
            return importSkuPricesCsv(file);
        } else {
            List<SkuPriceEntity> toSave = new ArrayList<>();
            try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
                Sheet sheet = wb.getSheetAt(0);
                boolean headerSkipped = false;
                for (Row row : sheet) {
                    if (!headerSkipped) { headerSkipped = true; continue; }
                    if (row == null) continue;
                    String sku = getCellAsString(row, 0).trim();
                    if (sku.isEmpty()) continue;
                    BigDecimal purchasePrice = parseBigDecimal(getCellAsString(row, 1));
                    toSave.add(SkuPriceEntity.builder()
                            .sku(sku)
                            .purchasePrice(purchasePrice)
                            .updatedAt(LocalDateTime.now())
                            .build());
                }
            }
            // upsert simple: delete all and insert (simple baseline)
            skuPriceRepository.deleteAllInBatch();
            skuPriceRepository.saveAll(toSave);
            return toSave.size();
        }
    }

    // Helpers
    public boolean isCsv(MultipartFile file) {
        String name = file.getOriginalFilename();
        String ct = file.getContentType();
        return (name != null && name.toLowerCase().endsWith(".csv"))
                || (ct != null && (ct.equalsIgnoreCase("text/csv") || ct.equalsIgnoreCase("application/csv")));
    }

    private int importOrdersCsv(MultipartFile file) throws Exception {
        List<OrderEntity> toSave = new ArrayList<>();
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            var headerMap = parser.getHeaderMap();
            log.info("CSV header map: {}", headerMap);
            
            for (CSVRecord r : parser) {
                String orderId = getAny(r, headerMap,
                        List.of("orderId", "order id", "sub order no", "sub order number", "suborderno"), 0);
                String sku = getAny(r, headerMap, List.of("sku", "supplier sku", "product sku"), 1);
                int qty = parseIntFlexible(getAny(r, headerMap, List.of("quantity", "qty"), 2));
                String priceStr = getAny(r, headerMap,
                        List.of("supplier discounted price (incl gst and commision)",
                                "supplier discounted price (incl gst and commission)",
                                "supplier listed price (incl. gst + commission)",
                                "listing price", "unit price", "price"), 3);
                BigDecimal price = new BigDecimal(cleanNumeric(priceStr));
                LocalDate date = parseToLocalDate(getAny(r, headerMap, List.of("order date", "date", "orderdate"), 4));
                
                // Get additional fields
                String productName = getAny(r, headerMap, List.of("product name", "product"), 5);
                String customerState = getAny(r, headerMap, List.of("customer state", "state"), 6);
                String size = getAny(r, headerMap, List.of("size"), 7);
                String supplierListedPriceStr = getAny(r, headerMap, List.of("supplier listed price (incl. gst + commission)"), 8);
                BigDecimal supplierListedPrice = parseBigDecimal(supplierListedPriceStr);
                String supplierDiscountedPriceStr = getAny(r, headerMap, List.of("supplier discounted price (incl gst and commision)", "supplier discounted price (incl gst and commission)"), 9);
                BigDecimal supplierDiscountedPrice = parseBigDecimal(supplierDiscountedPriceStr);
                String packetId = getAny(r, headerMap, List.of("packet id", "packet id"), 10);
                String reasonForCreditEntry = getAny(r, headerMap, List.of("reason for credit entry", "credit entry reason"), 11);
                
                toSave.add(OrderEntity.builder()
                        .orderId(orderId)
                        .sku(sku)
                        .quantity(qty)
                        .sellingPrice(price)
                        .orderDateTime(date.atStartOfDay())
                        .productName(productName)
                        .customerState(customerState)
                        .size(size)
                        .supplierListedPrice(supplierListedPrice)
                        .supplierDiscountedPrice(supplierDiscountedPrice)
                        .packetId(packetId)
                        .reasonForCreditEntry(reasonForCreditEntry)
                        .build());
            }
        }
        
        log.info("Total CSV order entities to process: {}", toSave.size());
        
        // Handle duplicates by using upsert logic
        int savedCount = 0;
        for (OrderEntity order : toSave) {
            try {
                // Check if order already exists
                var existingOrder = orderRepository.findByOrderId(order.getOrderId());
                if (existingOrder.isPresent()) {
                    log.info("CSV Order {} already exists, updating...", order.getOrderId());
                    var existing = existingOrder.get();
                    // Update all fields
                    existing.setSku(order.getSku());
                    existing.setQuantity(order.getQuantity());
                    existing.setSellingPrice(order.getSellingPrice());
                    existing.setOrderDateTime(order.getOrderDateTime());
                    existing.setProductName(order.getProductName());
                    existing.setCustomerState(order.getCustomerState());
                    existing.setSize(order.getSize());
                    existing.setSupplierListedPrice(order.getSupplierListedPrice());
                    existing.setSupplierDiscountedPrice(order.getSupplierDiscountedPrice());
                    existing.setPacketId(order.getPacketId());
                    existing.setReasonForCreditEntry(order.getReasonForCreditEntry());
                    orderRepository.save(existing);
                    savedCount++;
                } else {
                    // New order, save it
                    orderRepository.save(order);
                    savedCount++;
                }
            } catch (Exception e) {
                log.error("Error processing CSV order {}: {}", order.getOrderId(), e.getMessage());
                throw e;
            }
        }
        
        log.info("Successfully processed {} CSV order entities", savedCount);
        return savedCount;
    }

    private int importPaymentsCsv(MultipartFile file) throws Exception {
        List<PaymentEntity> toSave = new ArrayList<>();
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            var headerMap = parser.getHeaderMap();
            for (CSVRecord r : parser) {
                String paymentId = clamp(getAny(r, headerMap, List.of("payment id", "paymentId", "transaction id"), 0), "payment_id");
                String orderId = clamp(getAny(r, headerMap, List.of("order id", "orderId", "sub order no", "sub order number"), 1), "order_id");
                String amtStr = getAny(r, headerMap, List.of("final settlement amount", "net settlement amount", "amount"), 2);
                BigDecimal amount;
                try {
                    amount = new BigDecimal(cleanNumeric(amtStr));
                } catch (Exception ex) {
                    amount = BigDecimal.ZERO;
                    warn("CSV: orderId=" + orderId + ": Amount '" + amtStr + "' invalid; set to 0");
                }
                LocalDate date = parseToLocalDate(getAny(r, headerMap, List.of("payment date", "settlement date", "date"), 3));
                if (date == null) {
                    date = LocalDate.now();
                    warn("CSV: orderId=" + orderId + ": Payment date missing/invalid; set to today");
                }
                String orderStatus = clamp(getAny(r, headerMap, List.of("live order status", "order status", "status"), 4), "order_status");
                
                // Validate that order status is not null or blank
                if (orderStatus == null || orderStatus.isBlank()) {
                    orderStatus = "UNKNOWN";
                    warn("CSV: orderId=" + orderId + ": Missing order status; set to UNKNOWN");
                }
                
                // Enrich with order SKU and order date
                String skuForOrder = null;
                LocalDateTime orderDateTimeVal = null;
                try {
                    var orderOpt = orderRepository.findByOrderId(orderId);
                    if (orderOpt.isPresent()) {
                        skuForOrder = clamp(orderOpt.get().getSku(), "sku");
                        orderDateTimeVal = orderOpt.get().getOrderDateTime();
                    }
                } catch (Exception ignored) {}

                toSave.add(PaymentEntity.builder()
                        .paymentId(paymentId)
                        .orderId(orderId)
                        .sku(skuForOrder)
                        .amount(amount)
                        .paymentDateTime(date != null ? date.atStartOfDay() : null)
                        .orderDateTime(orderDateTimeVal)
                        .orderStatus(orderStatus)
                        .build());
            }
        }
        
        log.info("Total CSV payment entities to process: {}", toSave.size());
        
        // Handle duplicates by using upsert logic based on order_id (Sub Order No)
        int savedCount = 0;
        for (PaymentEntity payment : toSave) {
            try {
                // Check if payment already exists based on order_id (Sub Order No)
                var existingPayment = paymentRepository.findByOrderId(payment.getOrderId());
                if (existingPayment.isPresent()) {
                    log.info("CSV Payment for order {} already exists, updating...", payment.getOrderId());
                    var existing = existingPayment.get();
                    // Update all fields
                    existing.setPaymentId(payment.getPaymentId());
                    existing.setAmount(payment.getAmount());
                    existing.setPaymentDateTime(payment.getPaymentDateTime());
                    existing.setOrderDateTime(payment.getOrderDateTime());
                    existing.setOrderStatus(payment.getOrderStatus());
                    existing.setSku(payment.getSku());
                    paymentRepository.save(existing);
                    savedCount++;
                } else {
                    // New payment, save it
                    paymentRepository.save(payment);
                    savedCount++;
                }
            } catch (Exception e) {
                log.error("Error processing CSV payment for order {}: {}", payment.getOrderId(), e.getMessage());
                throw e;
            }
        }
        
        return savedCount;
    }

    private int importSkuPricesCsv(MultipartFile file) throws Exception {
        List<SkuPriceEntity> toSave = new ArrayList<>();
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            var headerMap = parser.getHeaderMap();
            for (CSVRecord r : parser) {
                String sku = getAny(r, headerMap, List.of("sku", "purchasePrice"), 0);
                String purchasePrice = getAny(r, headerMap, List.of("purchasePrice", "purchase price"), 1);
                toSave.add(SkuPriceEntity.builder()
                        .sku(sku)
                        .purchasePrice(new BigDecimal(cleanNumeric(purchasePrice)))
                        .updatedAt(LocalDateTime.now())
                        .build());
            }
        }
        skuPriceRepository.deleteAllInBatch();
        skuPriceRepository.saveAll(toSave);
        return toSave.size();
    }

    private String getAny(CSVRecord r, Map<String, Integer> headerMap, List<String> headerSynonyms, Integer fallbackIndex) {
        for (String h : headerSynonyms) {
            Integer idx = findHeaderIndex(headerMap, h);
            if (idx != null) {
                String v = r.get(idx);
                if (v != null && !v.isBlank()) return v.trim();
            }
        }
        if (fallbackIndex != null && fallbackIndex >= 0 && fallbackIndex < r.size()) {
            return r.get(fallbackIndex);
        }
        return "";
    }

    private Integer findHeaderIndex(Map<String, Integer> headerMap, String candidate) {
        if (headerMap == null) return null;
        String target = normalizeHeader(candidate);
        for (Map.Entry<String,Integer> e : headerMap.entrySet()) {
            if (normalizeHeader(e.getKey()).equals(target)) {
                return e.getValue();
            }
        }
        return null;
    }

    private String normalizeHeader(String h) {
        return (h == null ? "" : h).trim().toLowerCase().replaceAll("[\n\r\t]+", " ").replaceAll("\\s+", " ");
    }

    public BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(cleanNumeric(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public LocalDate parseToLocalDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public int parseIntFlexible(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return (int) Double.parseDouble(cleanNumeric(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String cleanNumeric(String value) {
        if (value == null) return "";
        return value.replaceAll("[₹,]", "").trim();
    }

    private Row findHeaderRow(Sheet sheet, List<String>... tokenGroups) {
        log.debug("findHeaderRow: looking for token groups: {}", (Object[]) tokenGroups);
        int limit = Math.min(sheet.getFirstRowNum() + 15, sheet.getLastRowNum());
        for (int r = sheet.getFirstRowNum(); r <= limit; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String joined = new StringBuilder()
                    .append(getCellAsString(row, row.getFirstCellNum() >= 0 ? row.getFirstCellNum() : 0))
                    .toString();
            // Basic scan across row
            boolean found = false;
            for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
                String cell = getCellAsString(row, c);
                joined += " " + cell;
            }
            String norm = normalizeHeader(joined);
            log.debug("Row {}: normalized content: '{}'", r, norm);
            // A row is header-like if it contains at least one token from EACH token group
            found = true;
            for (List<String> group : tokenGroups) {
                boolean anyInGroup = false;
                for (String t : group) {
                    if (norm.contains(normalizeHeader(t))) { anyInGroup = true; break; }
                }
                if (!anyInGroup) { found = false; break; }
            }
            if (found) {
                log.debug("Row {}: found header row", r);
                return row;
            }
        }
        log.debug("No header row found, returning first row");
        return sheet.getRow(sheet.getFirstRowNum());
    }

    private final DataFormatter dataFormatter = new DataFormatter();

    private String getCellAsString(Row row, int idx) {
        if (row == null || idx < 0) return "";
        try {
            var cell = row.getCell(idx);
            if (cell == null) return "";
            return dataFormatter.formatCellValue(cell);
        } catch (Exception e) {
            log.debug("Error getting cell value at index {}: {}", idx, e.getMessage());
            return "";
        }
    }

    private Map<String,Integer> buildHeaderIndex(Row header) {
        Map<String,Integer> map = new java.util.HashMap<>();
        log.debug("Building header index for row: {}", header.getRowNum());
        
        // More robust column scanning - scan a wider range for complex Excel files
        int maxCols = 100; // Increased from 50 to handle files with many columns
        for (int i = 0; i < maxCols; i++) {
            var cell = header.getCell(i);
            if (cell == null) continue;
            
            String cellValue = cell.getStringCellValue();
            if (cellValue == null || cellValue.trim().isEmpty()) continue;
            
            String name = normalizeHeader(cellValue);
            log.debug("Column {}: '{}' -> '{}'", i, cellValue, name);
            map.put(name, i);
        }
        log.debug("Final header map: {}", map);
        return map;
    }

    private String getCellAny(Row row, Map<String,Integer> hmap, List<String> names, Integer fallbackIdx) {
        log.debug("getCellAny: looking for names: {}, fallback: {}", names, fallbackIdx);
        
        // First try to resolve by header names
        for (String n : names) {
            Integer idx = resolveHeaderIndex(hmap, n);
            log.debug("getCellAny: name '{}' resolved to index: {}", n, idx);
            if (idx != null) {
                var cell = row.getCell(idx);
                if (cell != null) {
                    String v = getCellAsString(row, idx);
                    log.debug("getCellAny: cell at index {} = '{}'", idx, v);
                    if (v != null && !v.isBlank()) return v.trim();
                }
            }
        }
        
        // If header resolution failed, try fallback index
        if (fallbackIdx != null) {
            String v = getCellAsString(row, fallbackIdx);
            log.debug("getCellAny: fallback cell at index {} = '{}'", fallbackIdx, v);
            if (v != null && !v.isBlank()) return v.trim();
        }
        
        // Last resort: try common column positions based on typical Excel layouts
        if (names.contains("Sub Order No") || names.contains("Order Id")) {
            // Try column 1 (B) for order ID
            String v = getCellAsString(row, 1);
            if (v != null && !v.isBlank()) {
                log.debug("getCellAny: using fallback column 1 for order ID: '{}'", v);
                return v.trim();
            }
        }
        
        if (names.contains("Transaction ID") || names.contains("Payment Id")) {
            // Try column 9 (J) for transaction ID
            String v = getCellAsString(row, 9);
            if (v != null && !v.isBlank()) {
                log.debug("getCellAny: using fallback column 9 for transaction ID: '{}'", v);
                return v.trim();
            }
        }
        
        if (names.contains("Final Settlement Amount") || names.contains("Amount")) {
            // Try column 12 (L) for amount
            String v = getCellAsString(row, 12);
            if (v != null && !v.isBlank()) {
                log.debug("getCellAny: using fallback column 12 for amount: '{}'", v);
                return v.trim();
            }
        }
        
        if (names.contains("Payment Date") || names.contains("Date")) {
            // Try column 10 (K) for date
            String v = getCellAsString(row, 10);
            if (v != null && !v.isBlank()) {
                log.debug("getCellAny: using fallback column 10 for date: '{}'", v);
                return v.trim();
            }
        }
        
        log.debug("getCellAny: no value found for names: {}", names);
        return "";
    }

    private Integer resolveHeaderIndex(Map<String,Integer> hmap, String candidate) {
        String target = normalizeHeader(candidate);
        log.debug("resolveHeaderIndex: looking for '{}' (normalized: '{}')", candidate, target);
        Integer exact = hmap.get(target);
        if (exact != null) return exact;
        // fuzzy contains match
        for (Map.Entry<String,Integer> e : hmap.entrySet()) {
            String k = e.getKey();
            if (k.contains(target) || target.contains(k)) return e.getValue();
            // tokenized contains: all words in target exist in k
            String[] parts = target.split(" ");
            boolean all = true;
            for (String p : parts) { if (!p.isBlank() && !k.contains(p)) { all = false; break; } }
            if (all) return e.getValue();
        }
        log.debug("resolveHeaderIndex: no match found for '{}'", candidate);
        return null;
    }
}


