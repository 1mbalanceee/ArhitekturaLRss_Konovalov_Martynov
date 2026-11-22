package ru.ssau.jewelry.web;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.*;

import static org.junit.jupiter.api.Assertions.*;

class WebErrorsTest {
    private final WebErrors errors = new WebErrors();

    @Test
    void translatesCommitTimeConstraintErrorsWithoutSqlDetails() {
        var sql = new SQLException("internal SQL secret", "23505");
        assertTrue(errors.message(new DataIntegrityViolationException("secret",
                new ConstraintViolationException("secret", sql, "uq_categories_name"))).contains("названием"));
        assertTrue(errors.message(new DataIntegrityViolationException("secret",
                new ConstraintViolationException("secret", sql, "products_sku_key"))).contains("артикулом"));
        assertTrue(errors.message(new DataIntegrityViolationException("secret",
                new ConstraintViolationException("secret", sql, "products_category_id_fkey"))).contains("категори"));
        assertFalse(errors.message(new DataIntegrityViolationException("internal SQL secret")).contains("secret"));
    }

    @Test
    void translatesOptimisticLockFailure() {
        assertTrue(errors.message(new OptimisticLockingFailureException("secret")).contains("другой вкладке"));
    }
}
