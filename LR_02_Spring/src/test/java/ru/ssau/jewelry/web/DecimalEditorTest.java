package ru.ssau.jewelry.web;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DecimalEditorTest {
    private final DecimalEditor editor = new DecimalEditor();

    @ParameterizedTest
    @ValueSource(strings = {"0,123", "0.123", " 0,123 "})
    void acceptsBothSeparatorsWithoutPrecisionLoss(String value) {
        editor.setAsText(value);
        assertEquals(new BigDecimal("0.123"), editor.getValue());
        assertEquals("0,123", editor.getAsText());
    }

    @Test
    void preservesPricePrecisionAndScaleForValidation() {
        editor.setAsText("9876543210,12");
        assertEquals(new BigDecimal("9876543210.12"), editor.getValue());
        editor.setAsText("12,345");
        assertEquals(3, ((BigDecimal) editor.getValue()).scale());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1e3", "NaN", "1 234", "1,2.3", "1.2.3", "1,2,3", "12abc", "1 234,50", "+1"})
    void rejectsAmbiguousAndMalformedValues(String value) {
        assertThrows(IllegalArgumentException.class, () -> editor.setAsText(value));
    }

    @Test
    void handlesEmptyInput() {
        editor.setAsText(" ");
        assertNull(editor.getValue());
        assertEquals("", editor.getAsText());
    }
}
