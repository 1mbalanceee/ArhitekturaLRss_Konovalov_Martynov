package ru.ssau.jewelry.web;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.ssau.jewelry.dto.*;
import ru.ssau.jewelry.exception.*;
import ru.ssau.jewelry.service.*;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = {CategoryController.class, ProductController.class, HomeController.class},
        properties = "logging.level.ru.ssau.jewelry.web.ExceptionAdvice=OFF")
@Import(WebErrors.class)
class ControllersTest {
    @Autowired MockMvc mvc;
    @MockitoBean CategoryService categories;
    @MockitoBean ProductService products;

    @Test
    void homeRedirectsToProducts() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/products"));
    }

    @Test
    void pagesRenderRussianLayoutAndEmptyCategoryHint() throws Exception {
        mvc.perform(get("/products")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Ювелирный магазин")))
                .andExpect(content().string(containsString("создайте категорию")))
                .andExpect(content().string(containsString("Практическая работа №2")));
        mvc.perform(get("/categories")).andExpect(status().isOk()).andExpect(view().name("categories"));
    }

    @Test
    void categoryUsesPrgAndIgnoresUnlistedFields() throws Exception {
        mvc.perform(post("/categories/save").param("version", "0").param("name", "  Новая  ")
                        .param("description", "Описание").param("class.name", "ignored").param("products[0].id", "1"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/categories"))
                .andExpect(flash().attribute("success", "Категория сохранена"));
        ArgumentCaptor<CategoryForm> form = ArgumentCaptor.forClass(CategoryForm.class);
        verify(categories).save(form.capture());
        assertNull(form.getValue().getId());
        assertEquals("Новая", form.getValue().getName());
    }

    @Test
    void blankNameIsAnInlineErrorAndDoesNotReachService() throws Exception {
        mvc.perform(post("/categories/save").param("version", "0").param("name", "   "))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "name"))
                .andExpect(content().string(containsString("Введите название категории")));
        verify(categories, never()).save(any());
    }

    @Test
    void productBindsCommaAndDotAndDoesNotBindNestedEntity() throws Exception {
        mvc.perform(post("/products/save").param("version", "0")
                        .param("sku", " ring-001 ").param("name", "Кольцо").param("metal", "Золото")
                        .param("fineness", "585").param("stock", "0").param("categoryId", "1")
                        .param("weightGrams", "0,123").param("price", "9876543210.12")
                        .param("category.id", "999").param("category.version", "100"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/products"));
        ArgumentCaptor<ProductForm> form = ArgumentCaptor.forClass(ProductForm.class);
        verify(products).save(form.capture());
        assertEquals("RING-001", form.getValue().getSku());
        assertEquals(new BigDecimal("0.123"), form.getValue().getWeightGrams());
        assertEquals(new BigDecimal("9876543210.12"), form.getValue().getPrice());
        assertEquals(1L, form.getValue().getCategoryId());
    }

    @Test
    void bindingAndPrecisionErrorsPreserveEnteredValues() throws Exception {
        mvc.perform(post("/products/save").param("version", "0")
                        .param("sku", "R-1").param("name", "Кольцо").param("metal", "Золото")
                        .param("fineness", "bad").param("stock", "-1").param("categoryId", "1")
                        .param("weightGrams", "1,2,3").param("price", "12.345"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "fineness", "stock", "weightGrams", "price"))
                .andExpect(content().string(containsString("1,2,3")))
                .andExpect(content().string(containsString("Введите число с запятой или точкой")))
                .andExpect(content().string(containsString("без округления")));
        verify(products, never()).save(any());
    }

    @Test
    void deleteRequiresVersionAndUsesPost() throws Exception {
        mvc.perform(post("/products/5/delete")).andExpect(redirectedUrl("/products"))
                .andExpect(flash().attributeExists("error"));
        verify(products, never()).delete(anyLong(), anyLong());
        mvc.perform(post("/products/5/delete").param("version", "2"))
                .andExpect(redirectedUrl("/products")).andExpect(flash().attribute("success", "Изделие удалено"));
        verify(products).delete(5L, 2);
    }

    @Test
    void missingEditIdIsAReadable404() throws Exception {
        when(categories.findForm(99L)).thenThrow(new RecordNotFoundException("Категория не найдена"));
        mvc.perform(get("/categories/99/edit")).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Категория не найдена")));
    }

    @Test
    void unknownUrlsBadIdsAndGetDeleteHaveCorrectStatuses() throws Exception {
        mvc.perform(get("/missing-page")).andExpect(status().isNotFound());
        mvc.perform(get("/categories/not-a-number/edit")).andExpect(status().isBadRequest());
        mvc.perform(get("/products/5/delete")).andExpect(status().isMethodNotAllowed());
        verify(products, never()).delete(anyLong(), anyLong());
    }

    @Test
    void categoryBusinessErrorRetainsFormAndShowsFieldMessage() throws Exception {
        doThrow(new BusinessException("name", "Категория с таким названием уже существует"))
                .when(categories).save(any());
        mvc.perform(post("/categories/save").param("version", "0").param("name", "Кольца"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "name"))
                .andExpect(content().string(containsString("Категория с таким названием уже существует")));
    }

    @Test
    void commitTimeConflictRetainsProductFormAndShowsSafeMessage() throws Exception {
        doThrow(new OptimisticLockingFailureException("SQL secret")).when(products).save(any());
        mvc.perform(post("/products/save").param("version", "0")
                        .param("sku", "R-1").param("name", "Кольцо").param("metal", "Золото")
                        .param("fineness", "585").param("stock", "0").param("categoryId", "1")
                        .param("weightGrams", "0,123").param("price", "12.34"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("другой вкладке")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("SQL secret"))));
    }

    @Test
    void deleteConflictBecomesFlashError() throws Exception {
        doThrow(new BusinessException("Данные изменены")).when(products).delete(5L, 1);
        mvc.perform(post("/products/5/delete").param("version", "1"))
                .andExpect(redirectedUrl("/products")).andExpect(flash().attribute("error", "Данные изменены"));
    }

    @Test
    void unexpectedErrorNeverLeaksInternalDetails() throws Exception {
        when(products.findAll()).thenThrow(new IllegalStateException("secret SQL internal detail"));
        mvc.perform(get("/products")).andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Не удалось обработать запрос")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("secret SQL"))));
    }
}
