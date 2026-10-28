package ru.ssau.jewelry.web;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;
import java.math.BigDecimal;

@FacesConverter("decimal")
public class DecimalConverter implements Converter<BigDecimal> {
    @Override
    public BigDecimal getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace(',', '.');
        if (!normalized.matches("-?\\d+(?:\\.\\d+)?")) {
            throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Введите число, например 12,50. Разделители тысяч не используются", null));
        }
        return new BigDecimal(normalized);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, BigDecimal value) {
        return value == null ? "" : value.toPlainString().replace('.', ',');
    }
}
