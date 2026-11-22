package ru.ssau.jewelry.web;

import java.math.BigDecimal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice
public class FormBindingAdvice {
    @InitBinder
    public void decimalBinding(WebDataBinder binder) {
        binder.registerCustomEditor(BigDecimal.class, new DecimalEditor());
    }

    @InitBinder("deleteForm")
    public void deleteFields(WebDataBinder binder) {
        binder.setAllowedFields("version");
    }
}
