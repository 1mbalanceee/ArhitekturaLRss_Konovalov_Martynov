package ru.ssau.jewelry.service;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.ssau.jewelry.dto.CategoryForm;
import ru.ssau.jewelry.dto.CategoryViewDto;
import ru.ssau.jewelry.exception.BusinessException;
import ru.ssau.jewelry.exception.RecordNotFoundException;
import ru.ssau.jewelry.model.Category;
import ru.ssau.jewelry.repository.CategoryRepository;
import ru.ssau.jewelry.repository.ProductRepository;

@Service
@Validated
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categories;
    private final ProductRepository products;

    public CategoryService(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    public List<CategoryViewDto> findAll() {
        return categories.findAllByOrderByNameAsc().stream()
                .map(c -> new CategoryViewDto(c.getId(), c.getVersion(), c.getName(), c.getDescription()))
                .toList();
    }

    public CategoryForm findForm(Long id) {
        Category category = categories.findById(id).orElseThrow(CategoryService::missing);
        CategoryForm form = new CategoryForm();
        form.setId(category.getId());
        form.setVersion(category.getVersion());
        form.setName(category.getName());
        form.setDescription(category.getDescription());
        return form;
    }

    @Transactional
    public void save(@Valid CategoryForm form) {
        Category category = form.getId() == null ? new Category()
                : categories.findForChange(form.getId()).orElseThrow(CategoryService::missing);
        if (form.getId() != null) {
            checkVersion(category.getVersion(), form.getVersion());
        }
        if (categories.nameExists(form.getName(), form.getId() == null ? -1L : form.getId())) {
            throw new BusinessException("name", "Категория с таким названием уже существует");
        }
        category.setName(form.getName());
        category.setDescription(form.getDescription());
        if (form.getId() == null) {
            categories.save(category);
        }
    }

    @Transactional
    public void delete(Long id, long version) {
        Category category = categories.findForChange(id).orElseThrow(CategoryService::missing);
        checkVersion(category.getVersion(), version);
        if (products.existsByCategoryId(id)) {
            throw new BusinessException("В категории есть изделия. Сначала удалите их или смените категорию");
        }
        categories.delete(category);
    }

    private static RecordNotFoundException missing() {
        return new RecordNotFoundException("Категория не найдена или уже удалена. Обновите страницу");
    }

    private static void checkVersion(long actual, Long expected) {
        if (expected == null || actual != expected) {
            throw new BusinessException("Данные изменены в другой вкладке. Обновите страницу и повторите действие");
        }
    }
}
