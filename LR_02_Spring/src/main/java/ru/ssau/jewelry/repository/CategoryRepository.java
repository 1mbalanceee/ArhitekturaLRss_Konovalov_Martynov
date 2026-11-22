package ru.ssau.jewelry.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ru.ssau.jewelry.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByNameAsc();

    @Query("select count(c) > 0 from Category c where lower(c.name) = lower(:name) and c.id <> :id")
    boolean nameExists(@Param("name") String name, @Param("id") Long excludedId);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("select c from Category c where c.id = :id")
    Optional<Category> findForChange(@Param("id") Long id);
}
