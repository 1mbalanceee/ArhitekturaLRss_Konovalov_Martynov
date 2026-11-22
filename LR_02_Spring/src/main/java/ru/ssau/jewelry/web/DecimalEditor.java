package ru.ssau.jewelry.web;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;

public class DecimalEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) {
        if (text == null || text.isBlank()) {
            setValue(null);
            return;
        }
        String normalized = text.trim().replace(',', '.');
        if (!normalized.matches("-?\\d+(?:\\.\\d+)?")) {
            throw new IllegalArgumentException("Введите число с запятой или точкой, без разделителей тысяч");
        }
        setValue(new BigDecimal(normalized));
    }

    @Override
    public String getAsText() {
        BigDecimal value = (BigDecimal) getValue();
        return value == null ? "" : value.toPlainString().replace('.', ',');
    }
}
