package ru.ssau.jewelry.dto;

import java.math.BigDecimal;

public record ProductViewDto(Long id, long version, String sku, String name, String categoryName,
                             String metal, Integer fineness, BigDecimal weightGrams,
                             BigDecimal price, Integer stock) {
}
