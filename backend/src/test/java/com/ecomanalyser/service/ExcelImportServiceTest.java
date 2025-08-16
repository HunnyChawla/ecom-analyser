package com.ecomanalyser.service;

import com.ecomanalyser.domain.OrderEntity;
import com.ecomanalyser.domain.PaymentEntity;
import com.ecomanalyser.domain.SkuPriceEntity;
import com.ecomanalyser.repository.OrderRepository;
import com.ecomanalyser.repository.PaymentRepository;
import com.ecomanalyser.repository.SkuPriceRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExcelImportServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SkuPriceRepository skuPriceRepository;

    @InjectMocks
    private ExcelImportService excelImportService;

    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        mockFile = new MockMultipartFile(
            "file",
            "test.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "test content".getBytes()
        );
    }

    @Test
    void testIsCsvWithCsvFile() {
        // Given
        MockMultipartFile csvFile = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "test content".getBytes()
        );

        // When
        boolean result = excelImportService.isCsv(csvFile);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsCsvWithExcelFile() {
        // Given
        MockMultipartFile excelFile = new MockMultipartFile(
            "file",
            "test.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "test content".getBytes()
        );

        // When
        boolean result = excelImportService.isCsv(excelFile);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsCsvWithCsvExtension() {
        // Given
        MockMultipartFile csvFile = new MockMultipartFile(
            "file",
            "test.csv",
            "application/octet-stream",
            "test content".getBytes()
        );

        // When
        boolean result = excelImportService.isCsv(csvFile);

        // Then
        assertTrue(result);
    }

    @Test
    void testParseBigDecimalWithValidNumber() {
        // Given
        String validNumber = "123.45";

        // When
        BigDecimal result = excelImportService.parseBigDecimal(validNumber);

        // Then
        assertEquals(new BigDecimal("123.45"), result);
    }

    @Test
    void testParseBigDecimalWithInvalidNumber() {
        // Given
        String invalidNumber = "invalid";

        // When
        BigDecimal result = excelImportService.parseBigDecimal(invalidNumber);

        // Then
        assertNull(result);
    }

    @Test
    void testParseBigDecimalWithEmptyString() {
        // Given
        String emptyString = "";

        // When
        BigDecimal result = excelImportService.parseBigDecimal(emptyString);

        // Then
        assertNull(result);
    }

    @Test
    void testParseBigDecimalWithNull() {
        // Given
        String nullString = null;

        // When
        BigDecimal result = excelImportService.parseBigDecimal(nullString);

        // Then
        assertNull(result);
    }

    @Test
    void testParseToLocalDateWithValidDate() {
        // Given
        String validDate = "2025-08-15";

        // When
        LocalDate result = excelImportService.parseToLocalDate(validDate);

        // Then
        assertEquals(LocalDate.of(2025, 8, 15), result);
    }

    @Test
    void testParseToLocalDateWithInvalidDate() {
        // Given
        String invalidDate = "invalid-date";

        // When
        LocalDate result = excelImportService.parseToLocalDate(invalidDate);

        // Then
        assertNull(result);
    }

    @Test
    void testParseToLocalDateWithEmptyString() {
        // Given
        String emptyString = "";

        // When
        LocalDate result = excelImportService.parseToLocalDate(emptyString);

        // Then
        assertNull(result);
    }

    @Test
    void testParseToLocalDateWithNull() {
        // Given
        String nullString = null;

        // When
        LocalDate result = excelImportService.parseToLocalDate(nullString);

        // Then
        assertNull(result);
    }

    @Test
    void testParseIntFlexibleWithValidInteger() {
        // Given
        String validInteger = "123";

        // When
        int result = excelImportService.parseIntFlexible(validInteger);

        // Then
        assertEquals(123, result);
    }

    @Test
    void testParseIntFlexibleWithDecimal() {
        // Given
        String decimal = "123.45";

        // When
        int result = excelImportService.parseIntFlexible(decimal);

        // Then
        assertEquals(123, result);
    }

    @Test
    void testParseIntFlexibleWithInvalidNumber() {
        // Given
        String invalidNumber = "invalid";

        // When
        int result = excelImportService.parseIntFlexible(invalidNumber);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testParseIntFlexibleWithEmptyString() {
        // Given
        String emptyString = "";

        // When
        int result = excelImportService.parseIntFlexible(emptyString);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testParseIntFlexibleWithNull() {
        // Given
        String nullString = null;

        // When
        int result = excelImportService.parseIntFlexible(nullString);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testCleanNumericWithValidNumber() {
        // Given
        String validNumber = "₹123.45";

        // When
        String result = excelImportService.cleanNumeric(validNumber);

        // Then
        assertEquals("123.45", result);
    }

    @Test
    void testCleanNumericWithCommas() {
        // Given
        String numberWithCommas = "1,234.56";

        // When
        String result = excelImportService.cleanNumeric(numberWithCommas);

        // Then
        assertEquals("1234.56", result);
    }

    @Test
    void testCleanNumericWithSpaces() {
        // Given
        String numberWithSpaces = " 123.45 ";

        // When
        String result = excelImportService.cleanNumeric(numberWithSpaces);

        // Then
        assertEquals("123.45", result);
    }

    @Test
    void testCleanNumericWithEmptyString() {
        // Given
        String emptyString = "";

        // When
        String result = excelImportService.cleanNumeric(emptyString);

        // Then
        assertEquals("", result);
    }

    @Test
    void testCleanNumericWithNull() {
        // Given
        String nullString = null;

        // When
        String result = excelImportService.cleanNumeric(nullString);

        // Then
        assertEquals("", result);
    }
}
