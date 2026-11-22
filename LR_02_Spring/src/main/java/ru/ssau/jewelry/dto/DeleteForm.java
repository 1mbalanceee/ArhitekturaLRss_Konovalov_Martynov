package ru.ssau.jewelry.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class DeleteForm {
    @NotNull(message = "Не указана версия записи. Обновите страницу")
    @PositiveOrZero(message = "Некорректная версия записи")
    private Long version;

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
