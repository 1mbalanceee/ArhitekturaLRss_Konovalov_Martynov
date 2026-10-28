package ru.ssau.jewelry.web;

import jakarta.faces.convert.ConverterException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DecimalConverterTest {
    private final DecimalConverter converter = new DecimalConverter();

    @Test
    void priceDoesNotLosePrecision() {
        assertEquals(new BigDecimal("9876543210.12"), converter.getAsObject(null, null, "9876543210,12"));
    }

    @Test
    void dotAndCommaAreSupported() {
        assertEquals(new BigDecimal("0.123"), converter.getAsObject(null, null, "0,123"));
        assertEquals(new BigDecimal("0.123"), converter.getAsObject(null, null, "0.123"));
    }

    @Test
    void decimalScaleIsPreservedForValidation() {
        assertEquals(3, converter.getAsObject(null, null, "12,345").scale());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12abc", "1,2,3", "1 234", "1e3", "NaN", "1.2.3"})
    void malformedValuesAreRejected(String value) {
        assertThrows(ConverterException.class, () -> converter.getAsObject(null, null, value));
    }

    @Test
    void emptyValueIsHandledByRequiredValidation() {
        assertNull(converter.getAsObject(null, null, " "));
        assertNull(converter.getAsObject(null, null, null));
    }

    @Test
    void formattingDoesNotRound() {
        assertEquals("0,001", converter.getAsString(null, null, new BigDecimal("0.001")));
    }
}
