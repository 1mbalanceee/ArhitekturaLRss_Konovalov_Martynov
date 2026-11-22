package ru.ssau.jewelry;

import java.math.BigDecimal;
import ru.ssau.jewelry.dto.*;
import ru.ssau.jewelry.model.*;

public final class TestData {
    private TestData() {}

    public static CategoryForm categoryForm(Long id, long version) {
        CategoryForm form = new CategoryForm();
        form.setId(id);
        form.setVersion(version);
        form.setName("Кольца");
        form.setDescription("Описание");
        return form;
    }

    public static ProductForm productForm(Long id, long version, Long categoryId) {
        ProductForm form = new ProductForm();
        form.setId(id);
        form.setVersion(version);
        form.setCategoryId(categoryId);
        form.setSku("RING-001");
        form.setName("Кольцо");
        form.setMetal("Золото");
        form.setFineness(585);
        form.setWeightGrams(new BigDecimal("0.123"));
        form.setPrice(new BigDecimal("9876543210.12"));
        form.setStock(5);
        return form;
    }

    public static Category category(Long id, long version) {
        Category category = new Category();
        category.setId(id);
        category.setVersion(version);
        category.setName("Кольца");
        return category;
    }

    public static Product product(Long id, long version) {
        Product product = new Product();
        product.setId(id);
        product.setVersion(version);
        product.setSku("OLD-001");
        product.setName("Старое название");
        product.setCategory(category(1L, 0));
        return product;
    }
}
