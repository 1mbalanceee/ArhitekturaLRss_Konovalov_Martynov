package ru.ssau.jewelry.service;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import ru.ssau.jewelry.model.Category;
import ru.ssau.jewelry.model.Product;

@Stateless
public class InventoryService {
    @PersistenceContext(unitName = "jewelryPU")
    private EntityManager entityManager;

    @Inject
    private Validator validator;

    public List<Category> findCategories() {
        return entityManager.createQuery("select c from Category c order by c.name", Category.class)
                .getResultList();
    }

    public List<Product> findProducts() {
        return entityManager.createQuery(
                "select p from Product p join fetch p.category order by p.id desc", Product.class)
                .getResultList();
    }

    public void saveCategory(Category input) {
        input.setName(trim(input.getName()));
        input.setDescription(trim(input.getDescription()));
        validate(input);
        long duplicates = entityManager.createQuery(
                "select count(c) from Category c where lower(c.name) = :name and c.id <> :id",
                Long.class)
                .setParameter("name", input.getName().toLowerCase(Locale.ROOT))
                .setParameter("id", input.getId() == null ? -1L : input.getId())
                .getSingleResult();
        if (duplicates > 0) {
            throw new BusinessException("Категория с таким названием уже существует");
        }
        if (input.getId() == null) {
            entityManager.persist(input);
        } else {
            Category category = requireCategory(input.getId(), LockModeType.OPTIMISTIC);
            checkVersion(category.getVersion(), input.getVersion());
            category.setName(input.getName());
            category.setDescription(input.getDescription());
        }
        entityManager.flush();
    }

    public void deleteCategory(Long id, long version) {
        Category category = requireCategory(id, LockModeType.PESSIMISTIC_WRITE);
        checkVersion(category.getVersion(), version);
        long products = entityManager.createQuery(
                "select count(p) from Product p where p.category.id = :id", Long.class)
                .setParameter("id", id).getSingleResult();
        if (products > 0) {
            throw new BusinessException("В категории есть изделия. Сначала удалите их или смените категорию");
        }
        entityManager.remove(category);
        entityManager.flush();
    }

    public void saveProduct(Product input, Long categoryId) {
        Category category = requireCategory(categoryId, LockModeType.PESSIMISTIC_WRITE);
        input.setCategory(category);
        input.setSku(trim(input.getSku()).toUpperCase(Locale.ROOT));
        input.setName(trim(input.getName()));
        input.setMetal(trim(input.getMetal()));
        validate(input);
        long duplicates = entityManager.createQuery(
                "select count(p) from Product p where p.sku = :sku and p.id <> :id", Long.class)
                .setParameter("sku", input.getSku())
                .setParameter("id", input.getId() == null ? -1L : input.getId()).getSingleResult();
        if (duplicates > 0) {
            throw new BusinessException("Изделие с таким артикулом уже существует");
        }
        if (input.getId() == null) {
            entityManager.persist(input);
        } else {
            Product product = entityManager.find(Product.class, input.getId(), LockModeType.OPTIMISTIC);
            if (product == null) {
                throw new BusinessException("Изделие уже удалено. Обновите страницу");
            }
            checkVersion(product.getVersion(), input.getVersion());
            product.setSku(input.getSku());
            product.setName(input.getName());
            product.setMetal(input.getMetal());
            product.setFineness(input.getFineness());
            product.setWeightGrams(input.getWeightGrams());
            product.setPrice(input.getPrice());
            product.setStock(input.getStock());
            product.setCategory(category);
        }
        entityManager.flush();
    }

    public void deleteProduct(Long id, long version) {
        Product product = entityManager.find(Product.class, id, LockModeType.OPTIMISTIC);
        if (product == null) {
            throw new BusinessException("Изделие уже удалено. Обновите страницу");
        }
        checkVersion(product.getVersion(), version);
        entityManager.remove(product);
        entityManager.flush();
    }

    private Category requireCategory(Long id, LockModeType lockMode) {
        if (id == null) {
            throw new BusinessException("Выберите категорию");
        }
        Category category = entityManager.find(Category.class, id, lockMode);
        if (category == null) {
            throw new BusinessException("Категория уже удалена. Обновите страницу");
        }
        return category;
    }

    private void checkVersion(long actual, long expected) {
        if (actual != expected) {
            throw new BusinessException("Данные изменены в другой вкладке. Обновите страницу и повторите действие");
        }
    }

    private <T> void validate(T bean) {
        Set<ConstraintViolation<T>> violations = validator.validate(bean);
        if (!violations.isEmpty()) {
            String message = violations.stream().map(ConstraintViolation::getMessage)
                    .distinct().sorted().collect(Collectors.joining(". "));
            throw new BusinessException(message);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
