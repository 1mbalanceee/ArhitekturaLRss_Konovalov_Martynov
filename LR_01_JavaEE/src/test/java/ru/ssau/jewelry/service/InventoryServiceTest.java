package ru.ssau.jewelry.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.ssau.jewelry.model.Category;
import ru.ssau.jewelry.model.Product;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InventoryServiceTest {
    private InventoryService service;
    private EntityManager entityManager;
    private Validator validator;
    private TypedQuery<Long> countQuery;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws ReflectiveOperationException {
        service = new InventoryService();
        entityManager = mock(EntityManager.class);
        validator = mock(Validator.class);
        countQuery = mock(TypedQuery.class);
        inject("entityManager", entityManager);
        inject("validator", validator);
        when(validator.validate(any())).thenReturn(Set.of());
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
    }

    @Test
    void categoryWithProductsCannotBeDeleted() {
        Category category = category(1L, 0);
        when(entityManager.find(Category.class, 1L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(category);
        when(countQuery.getSingleResult()).thenReturn(2L);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.deleteCategory(1L, 0));

        assertTrue(exception.getMessage().contains("есть изделия"));
        verify(entityManager, never()).remove(any());
    }

    @Test
    void emptyCategoryCanBeDeleted() {
        Category category = category(1L, 0);
        when(entityManager.find(Category.class, 1L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(category);

        service.deleteCategory(1L, 0);

        verify(entityManager).remove(category);
        verify(entityManager).flush();
    }

    @Test
    void duplicateCategoryIsRejected() {
        Category category = category(null, 0);
        when(countQuery.getSingleResult()).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.saveCategory(category));

        verify(entityManager, never()).persist(any());
    }

    @Test
    void staleCategoryCannotOverwriteNewerData() {
        Category managed = category(1L, 3);
        Category input = category(1L, 2);
        input.setName("Новое название");
        when(entityManager.find(Category.class, 1L, LockModeType.OPTIMISTIC)).thenReturn(managed);

        assertThrows(BusinessException.class, () -> service.saveCategory(input));

        assertEquals("Кольца", managed.getName());
        verify(entityManager, never()).flush();
    }

    @Test
    void categoryUpdateUsesManagedEntity() {
        Category managed = category(1L, 2);
        Category input = category(1L, 2);
        input.setName("  Серьги  ");
        when(entityManager.find(Category.class, 1L, LockModeType.OPTIMISTIC)).thenReturn(managed);

        service.saveCategory(input);

        assertEquals("Серьги", managed.getName());
        verify(entityManager, never()).merge(any());
        verify(entityManager).flush();
    }

    @Test
    void productCannotReferenceDeletedCategory() {
        assertThrows(BusinessException.class, () -> service.saveProduct(product(null, 0), 99L));
        verify(entityManager, never()).persist(any());
    }

    @Test
    void duplicateSkuIsRejectedAfterNormalization() {
        when(entityManager.find(Category.class, 1L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(category(1L, 0));
        when(countQuery.getSingleResult()).thenReturn(1L);
        Product input = product(null, 0);
        input.setSku(" ring-001 ");

        assertThrows(BusinessException.class, () -> service.saveProduct(input, 1L));

        verify(countQuery).setParameter("sku", "RING-001");
        verify(entityManager, never()).persist(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void invalidProductDoesNotReachPersistence() {
        Product input = product(null, 0);
        input.setPrice(new BigDecimal("-1"));
        ConstraintViolation<Product> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Цена должна быть больше нуля");
        when(validator.validate(input)).thenReturn(Set.of(violation));
        when(entityManager.find(Category.class, 1L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(category(1L, 0));

        assertThrows(BusinessException.class, () -> service.saveProduct(input, 1L));

        verify(entityManager, never()).persist(any());
    }

    @Test
    void productUpdatePreservesExactDecimalValuesAndCategory() {
        Product input = product(5L, 2);
        Product managed = product(5L, 2);
        Category targetCategory = category(2L, 0);
        input.setPrice(new BigDecimal("9876543210.12"));
        input.setWeightGrams(new BigDecimal("0.123"));
        when(entityManager.find(Category.class, 2L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(targetCategory);
        when(entityManager.find(Product.class, 5L, LockModeType.OPTIMISTIC)).thenReturn(managed);

        service.saveProduct(input, 2L);

        assertEquals(new BigDecimal("9876543210.12"), managed.getPrice());
        assertEquals(new BigDecimal("0.123"), managed.getWeightGrams());
        assertSame(targetCategory, managed.getCategory());
    }

    @Test
    void staleProductCannotBeDeleted() {
        when(entityManager.find(Product.class, 5L, LockModeType.OPTIMISTIC)).thenReturn(product(5L, 3));

        assertThrows(BusinessException.class, () -> service.deleteProduct(5L, 2));

        verify(entityManager, never()).remove(any());
    }

    @Test
    void productCanBeDeleted() {
        Product product = product(5L, 2);
        when(entityManager.find(Product.class, 5L, LockModeType.OPTIMISTIC)).thenReturn(product);

        service.deleteProduct(5L, 2);

        verify(entityManager).remove(product);
        verify(entityManager).flush();
    }

    private Category category(Long id, long version) {
        Category category = new Category();
        category.setId(id);
        category.setVersion(version);
        category.setName("Кольца");
        return category;
    }

    private Product product(Long id, long version) {
        Product product = new Product();
        product.setId(id);
        product.setVersion(version);
        product.setSku("RING-001");
        product.setName("Кольцо");
        product.setMetal("Золото");
        product.setFineness(585);
        product.setPrice(new BigDecimal("12345.67"));
        product.setWeightGrams(new BigDecimal("3.200"));
        product.setStock(5);
        return product;
    }

    private void inject(String name, Object value) throws ReflectiveOperationException {
        Field field = InventoryService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
