package ru.ssau.jewelry.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ssau.jewelry.dto.CategoryForm;
import ru.ssau.jewelry.exception.*;
import ru.ssau.jewelry.model.Category;
import ru.ssau.jewelry.repository.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ru.ssau.jewelry.TestData.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock CategoryRepository categories;
    @Mock ProductRepository products;
    CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categories, products);
    }

    @Test
    void createsCategoryWithNormalizedText() {
        CategoryForm form = categoryForm(null, 0);
        form.setName("  Кольца  ");
        form.setDescription("  Описание  ");
        service.save(form);
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categories).save(captor.capture());
        assertNull(captor.getValue().getId());
        assertEquals("Кольца", captor.getValue().getName());
        assertEquals("Описание", captor.getValue().getDescription());
        verify(categories).nameExists("Кольца", -1L);
    }

    @Test
    void editsOnlyAllowedPropertiesOfManagedEntity() {
        Category managed = category(1L, 4);
        when(categories.findForChange(1L)).thenReturn(Optional.of(managed));
        CategoryForm form = categoryForm(1L, 4);
        form.setName("Серьги");
        service.save(form);
        assertEquals("Серьги", managed.getName());
        assertEquals(1L, managed.getId());
        assertEquals(4L, managed.getVersion());
        verify(categories).nameExists("Серьги", 1L);
        verify(categories, never()).save(any());
    }

    @Test
    void rejectsDuplicateName() {
        when(categories.nameExists("Кольца", -1L)).thenReturn(true);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.save(categoryForm(null, 0)));
        assertEquals("name", exception.getField());
        verify(categories, never()).save(any());
    }

    @Test
    void deletesEmptyCategory() {
        Category managed = category(1L, 3);
        when(categories.findForChange(1L)).thenReturn(Optional.of(managed));
        service.delete(1L, 3);
        verify(categories).delete(managed);
    }

    @Test
    void refusesToDeleteCategoryWithProducts() {
        when(categories.findForChange(1L)).thenReturn(Optional.of(category(1L, 0)));
        when(products.existsByCategoryId(1L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.delete(1L, 0));
        verify(categories, never()).delete(any(Category.class));
    }

    @Test
    void staleEditDoesNotModifyEntity() {
        Category managed = category(1L, 3);
        when(categories.findForChange(1L)).thenReturn(Optional.of(managed));
        CategoryForm form = categoryForm(1L, 2);
        form.setName("Новое");
        assertThrows(BusinessException.class, () -> service.save(form));
        assertEquals("Кольца", managed.getName());
    }

    @Test
    void staleDeleteIsRejected() {
        when(categories.findForChange(1L)).thenReturn(Optional.of(category(1L, 3)));
        assertThrows(BusinessException.class, () -> service.delete(1L, 2));
        verify(categories, never()).delete(any(Category.class));
    }

    @Test
    void handlesMissingIdsForReadEditAndDelete() {
        assertThrows(RecordNotFoundException.class, () -> service.findForm(99L));
        assertThrows(RecordNotFoundException.class, () -> service.save(categoryForm(99L, 0)));
        assertThrows(RecordNotFoundException.class, () -> service.delete(99L, 0));
    }

    @Test
    void returnsDtosForSortedListAndForm() {
        Category category = category(1L, 2);
        when(categories.findAllByOrderByNameAsc()).thenReturn(List.of(category));
        when(categories.findById(1L)).thenReturn(Optional.of(category));
        assertEquals("Кольца", service.findAll().getFirst().name());
        assertEquals(2L, service.findForm(1L).getVersion());
    }
}
