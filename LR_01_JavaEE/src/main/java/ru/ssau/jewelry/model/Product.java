package ru.ssau.jewelry.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @NotBlank(message = "Введите артикул")
    @Pattern(regexp = "[A-Za-z0-9-]{1,40}", message = "Артикул: латинские буквы, цифры и дефис, до 40 символов")
    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @NotBlank(message = "Введите название изделия")
    @Size(max = 150, message = "Название изделия: не более 150 символов")
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank(message = "Укажите металл")
    @Size(max = 50, message = "Металл: не более 50 символов")
    @Column(nullable = false, length = 50)
    private String metal;

    @NotNull(message = "Укажите пробу")
    @Min(value = 1, message = "Проба должна быть от 1 до 999")
    @Max(value = 999, message = "Проба должна быть от 1 до 999")
    @Column(nullable = false)
    private Integer fineness;

    @NotNull(message = "Укажите вес")
    @DecimalMin(value = "0.001", message = "Вес должен быть больше нуля")
    @Digits(integer = 7, fraction = 3, message = "Вес: до 7 цифр и 3 знаков после запятой")
    @Column(name = "weight_grams", nullable = false, precision = 10, scale = 3)
    private BigDecimal weightGrams;

    @NotNull(message = "Укажите цену")
    @DecimalMin(value = "0.01", message = "Цена должна быть больше нуля")
    @Digits(integer = 10, fraction = 2, message = "Цена: до 10 цифр и 2 знаков после запятой")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Укажите остаток")
    @Min(value = 0, message = "Остаток не может быть отрицательным")
    @Column(nullable = false)
    private Integer stock = 0;

    @NotNull(message = "Выберите категорию")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    public Product() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMetal() { return metal; }
    public void setMetal(String metal) { this.metal = metal; }
    public Integer getFineness() { return fineness; }
    public void setFineness(Integer fineness) { this.fineness = fineness; }
    public BigDecimal getWeightGrams() { return weightGrams; }
    public void setWeightGrams(BigDecimal weightGrams) { this.weightGrams = weightGrams; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
}
