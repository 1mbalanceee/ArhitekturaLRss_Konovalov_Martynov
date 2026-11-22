package ru.ssau.jewelry.dto;

import jakarta.validation.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static ru.ssau.jewelry.TestData.*;

class FormValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void validFormsHaveNoViolations() {
        assertTrue(validator.validate(categoryForm(null, 0)).isEmpty());
        assertTrue(validator.validate(productForm(null, 0, 1L)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0", "1.234", "10000000000.00"})
    void rejectsInvalidPriceWithoutRounding(String value) {
        ProductForm form = productForm(null, 0, 1L);
        form.setPrice(new BigDecimal(value));
        assertFieldInvalid(form, "price");
        assertEquals(new BigDecimal(value), form.getPrice());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0", "0.0001", "10000000.000"})
    void rejectsInvalidWeightWithoutRounding(String value) {
        ProductForm form = productForm(null, 0, 1L);
        form.setWeightGrams(new BigDecimal(value));
        assertFieldInvalid(form, "weightGrams");
    }

    @Test
    void rejectsNegativeStockAndOutOfRangeFineness() {
        ProductForm form = productForm(null, 0, 1L);
        form.setStock(-1);
        form.setFineness(1000);
        assertFieldInvalid(form, "stock");
        assertFieldInvalid(form, "fineness");
        form.setFineness(0);
        assertFieldInvalid(form, "fineness");
    }

    @Test
    void validatesTextLimitsRequiredValuesAndVersion() {
        CategoryForm category = categoryForm(null, 0);
        category.setName("   ");
        category.setDescription("a".repeat(501));
        category.setVersion(null);
        assertFieldInvalid(category, "name");
        assertFieldInvalid(category, "description");
        assertFieldInvalid(category, "version");
        category.setName("a".repeat(101));
        assertFieldInvalid(category, "name");
        ProductForm product = productForm(null, 0, null);
        product.setName("a".repeat(151));
        product.setMetal("a".repeat(51));
        product.setSku("артикул");
        assertFieldInvalid(product, "categoryId");
        assertFieldInvalid(product, "name");
        assertFieldInvalid(product, "metal");
        assertFieldInvalid(product, "sku");
    }

    @Test
    void normalizationPrecedesValidationAndUsesRootLocale() {
        ProductForm form = productForm(null, 0, 1L);
        form.setSku("  ring-i  ");
        assertEquals("RING-I", form.getSku());
        assertTrue(validator.validate(form).isEmpty());
    }

    private void assertFieldInvalid(Object form, String field) {
        assertTrue(validator.validate(form).stream().anyMatch(v -> v.getPropertyPath().toString().equals(field)),
                "Ожидалась ошибка поля " + field);
    }
}
