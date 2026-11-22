package ru.ssau.jewelry.dto;

import jakarta.validation.constraints.*;

public class CategoryForm {
    @Positive(message = "Некорректный ID категории")
    private Long id;
    @NotNull(message = "Не указана версия записи. Обновите страницу")
    @PositiveOrZero(message = "Некорректная версия записи")
    private Long version;
    @NotBlank(message = "Введите название категории")
    @Size(max = 100, message = "Название категории: не более 100 символов")
    private String name;
    @Size(max = 500, message = "Описание: не более 500 символов")
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? null : name.trim(); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? null : description.trim(); }
}
