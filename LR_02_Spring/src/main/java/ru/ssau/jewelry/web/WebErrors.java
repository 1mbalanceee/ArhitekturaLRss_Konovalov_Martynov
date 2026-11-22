package ru.ssau.jewelry.web;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import ru.ssau.jewelry.exception.BusinessException;

@Component
public class WebErrors {
    public void reject(BindingResult result, BusinessException exception) {
        if (exception.getField() == null) {
            result.reject("business", exception.getMessage());
        } else {
            result.rejectValue(exception.getField(), "business", exception.getMessage());
        }
    }

    public void reject(BindingResult result, DataAccessException exception) {
        result.reject("persistence", message(exception));
    }

    public String message(DataAccessException exception) {
        if (exception instanceof ConcurrencyFailureException) {
            return "Данные изменены или удалены в другой вкладке. Обновите страницу и повторите действие";
        }
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String constraint = violation.getConstraintName();
                if ("uq_categories_name".equals(constraint)) {
                    return "Категория с таким названием уже существует";
                }
                if ("products_sku_key".equals(constraint)) {
                    return "Изделие с таким артикулом уже существует";
                }
                if ("products_category_id_fkey".equals(constraint)) {
                    return "Связь с категорией изменилась. Категория должна существовать и не содержать изделий при удалении";
                }
            }
        }
        return "Не удалось выполнить операцию. Обновите страницу и проверьте данные и доступность базы";
    }
}
