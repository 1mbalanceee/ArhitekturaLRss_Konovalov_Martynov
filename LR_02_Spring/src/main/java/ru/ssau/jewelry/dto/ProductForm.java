package ru.ssau.jewelry.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Locale;

public class ProductForm {
    @Positive(message = "Некорректный ID изделия")
    private Long id;
    @NotNull(message = "Не указана версия записи. Обновите страницу")
    @PositiveOrZero(message = "Некорректная версия записи")
    private Long version;
    @NotBlank(message = "Введите артикул")
    @Pattern(regexp = "[A-Z0-9-]{1,40}", message = "Артикул: латинские буквы, цифры и дефис, до 40 символов")
    private String sku;
    @NotBlank(message = "Введите название изделия")
    @Size(max = 150, message = "Название изделия: не более 150 символов")
    private String name;
    @NotBlank(message = "Укажите металл")
    @Size(max = 50, message = "Металл: не более 50 символов")
    private String metal;
    @NotNull(message = "Укажите пробу")
    @Min(value = 1, message = "Проба должна быть от 1 до 999")
    @Max(value = 999, message = "Проба должна быть от 1 до 999")
    private Integer fineness;
    @NotNull(message = "Укажите вес")
    @DecimalMin(value = "0.001", message = "Вес должен быть больше нуля")
    @Digits(integer = 7, fraction = 3, message = "Вес: до 7 цифр и 3 знаков после запятой, без округления")
    private BigDecimal weightGrams;
    @NotNull(message = "Укажите цену")
    @DecimalMin(value = "0.01", message = "Цена должна быть больше нуля")
    @Digits(integer = 10, fraction = 2, message = "Цена: до 10 цифр и 2 знаков после запятой, без округления")
    private BigDecimal price;
    @NotNull(message = "Укажите остаток")
    @Min(value = 0, message = "Остаток не может быть отрицательным")
    private Integer stock = 0;
    @NotNull(message = "Выберите категорию")
    @Positive(message = "Выберите существующую категорию")
    private Long categoryId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku == null ? null : sku.trim().toUpperCase(Locale.ROOT); }
    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? null : name.trim(); }
    public String getMetal() { return metal; }
    public void setMetal(String metal) { this.metal = metal == null ? null : metal.trim(); }
    public Integer getFineness() { return fineness; }
    public void setFineness(Integer fineness) { this.fineness = fineness; }
    public BigDecimal getWeightGrams() { return weightGrams; }
    public void setWeightGrams(BigDecimal weightGrams) { this.weightGrams = weightGrams; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
