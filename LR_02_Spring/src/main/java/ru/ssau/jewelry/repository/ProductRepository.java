package ru.ssau.jewelry.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ru.ssau.jewelry.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = "category")
    List<Product> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Long id);

    boolean existsBySkuAndIdNot(String sku, Long excludedId);
    boolean existsByCategoryId(Long categoryId);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findForChange(@Param("id") Long id);
}
