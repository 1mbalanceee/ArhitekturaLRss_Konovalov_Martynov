package ru.ssau.jewelry.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ssau.jewelry.dto.ProductForm;
import ru.ssau.jewelry.exception.*;
import ru.ssau.jewelry.model.*;
import ru.ssau.jewelry.repository.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ru.ssau.jewelry.TestData.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository products;
    @Mock CategoryRepository categories;
    ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(products, categories);
    }

    @Test
    void createsProductWithNormalizedSkuAndExactDecimals() {
        Category category = category(1L, 0);
        when(categories.findById(1L)).thenReturn(Optional.of(category));
        ProductForm form = productForm(null, 0, 1L);
        form.setSku("  ring-001  ");
        form.setName("  Кольцо  ");
        form.setMetal("  Золото  ");
        service.save(form);
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(products).save(captor.capture());
        Product saved = captor.getValue();
        assertEquals("RING-001", saved.getSku());
        assertEquals("Кольцо", saved.getName());
        assertEquals("Золото", saved.getMetal());
        assertEquals(new BigDecimal("9876543210.12"), saved.getPrice());
        assertEquals(new BigDecimal("0.123"), saved.getWeightGrams());
        assertSame(category, saved.getCategory());
    }

    @Test
    void editsManagedProductAndCategoryWithoutChangingIdentity() {
        Product managed = product(5L, 2);
        Category target = category(2L, 0);
        when(products.findForChange(5L)).thenReturn(Optional.of(managed));
        when(categories.findById(2L)).thenReturn(Optional.of(target));
        service.save(productForm(5L, 2, 2L));
        assertEquals("RING-001", managed.getSku());
        assertEquals(new BigDecimal("9876543210.12"), managed.getPrice());
        assertEquals(new BigDecimal("0.123"), managed.getWeightGrams());
        assertSame(target, managed.getCategory());
        assertEquals(5L, managed.getId());
        assertEquals(2L, managed.getVersion());
        verify(products, never()).save(any());
    }

    @Test
    void duplicateNormalizedSkuIsRejected() {
        when(categories.findById(1L)).thenReturn(Optional.of(category(1L, 0)));
        when(products.existsBySkuAndIdNot("RING-001", -1L)).thenReturn(true);
        ProductForm form = productForm(null, 0, 1L);
        form.setSku(" ring-001 ");
        BusinessException exception = assertThrows(BusinessException.class, () -> service.save(form));
        assertEquals("sku", exception.getField());
        verify(products, never()).save(any());
    }

    @Test
    void refusesMissingCategory() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.save(productForm(null, 0, 99L)));
        assertEquals("categoryId", exception.getField());
        verify(products, never()).save(any());
    }

    @Test
    void deletesProduct() {
        Product managed = product(5L, 2);
        when(products.findForChange(5L)).thenReturn(Optional.of(managed));
        service.delete(5L, 2);
        verify(products).delete(managed);
    }

    @Test
    void staleEditDoesNotOverwriteData() {
        Product managed = product(5L, 3);
        when(products.findForChange(5L)).thenReturn(Optional.of(managed));
        assertThrows(BusinessException.class, () -> service.save(productForm(5L, 2, 1L)));
        assertEquals("OLD-001", managed.getSku());
        verifyNoInteractions(categories);
    }

    @Test
    void staleDeleteIsRejected() {
        when(products.findForChange(5L)).thenReturn(Optional.of(product(5L, 3)));
        assertThrows(BusinessException.class, () -> service.delete(5L, 2));
        verify(products, never()).delete(any(Product.class));
    }

    @Test
    void handlesMissingIdsForReadEditAndDelete() {
        assertThrows(RecordNotFoundException.class, () -> service.findForm(99L));
        assertThrows(RecordNotFoundException.class, () -> service.save(productForm(99L, 0, 1L)));
        assertThrows(RecordNotFoundException.class, () -> service.delete(99L, 0));
    }

    @Test
    void listAndEditorContainDtosWithCategory() {
        Product product = product(5L, 2);
        when(products.findAllByOrderByIdDesc()).thenReturn(List.of(product));
        when(products.findById(5L)).thenReturn(Optional.of(product));
        assertEquals("Кольца", service.findAll().getFirst().categoryName());
        assertEquals(1L, service.findForm(5L).getCategoryId());
    }
}
