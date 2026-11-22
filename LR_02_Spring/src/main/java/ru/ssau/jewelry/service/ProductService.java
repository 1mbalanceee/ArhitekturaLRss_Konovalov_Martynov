package ru.ssau.jewelry.service;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.ssau.jewelry.dto.ProductForm;
import ru.ssau.jewelry.dto.ProductViewDto;
import ru.ssau.jewelry.exception.BusinessException;
import ru.ssau.jewelry.exception.RecordNotFoundException;
import ru.ssau.jewelry.model.Category;
import ru.ssau.jewelry.model.Product;
import ru.ssau.jewelry.repository.CategoryRepository;
import ru.ssau.jewelry.repository.ProductRepository;

@Service
@Validated
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository products;
    private final CategoryRepository categories;

    public ProductService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    public List<ProductViewDto> findAll() {
        return products.findAllByOrderByIdDesc().stream()
                .map(p -> new ProductViewDto(p.getId(), p.getVersion(), p.getSku(), p.getName(),
                        p.getCategory().getName(), p.getMetal(), p.getFineness(), p.getWeightGrams(),
                        p.getPrice(), p.getStock()))
                .toList();
    }

    public ProductForm findForm(Long id) {
        Product product = products.findById(id).orElseThrow(ProductService::missing);
        ProductForm form = new ProductForm();
        form.setId(product.getId());
        form.setVersion(product.getVersion());
        form.setSku(product.getSku());
        form.setName(product.getName());
        form.setMetal(product.getMetal());
        form.setFineness(product.getFineness());
        form.setWeightGrams(product.getWeightGrams());
        form.setPrice(product.getPrice());
        form.setStock(product.getStock());
        form.setCategoryId(product.getCategory().getId());
        return form;
    }

    @Transactional
    public void save(@Valid ProductForm form) {
        Product product = form.getId() == null ? new Product()
                : products.findForChange(form.getId()).orElseThrow(ProductService::missing);
        if (form.getId() != null) {
            checkVersion(product.getVersion(), form.getVersion());
        }
        Category category = categories.findById(form.getCategoryId()).orElseThrow(() ->
                new BusinessException("categoryId", "Выбранная категория не существует. Выберите другую"));
        if (products.existsBySkuAndIdNot(form.getSku(), form.getId() == null ? -1L : form.getId())) {
            throw new BusinessException("sku", "Изделие с таким артикулом уже существует");
        }
        product.setSku(form.getSku());
        product.setName(form.getName());
        product.setMetal(form.getMetal());
        product.setFineness(form.getFineness());
        product.setWeightGrams(form.getWeightGrams());
        product.setPrice(form.getPrice());
        product.setStock(form.getStock());
        product.setCategory(category);
        if (form.getId() == null) {
            products.save(product);
        }
    }

    @Transactional
    public void delete(Long id, long version) {
        Product product = products.findForChange(id).orElseThrow(ProductService::missing);
        checkVersion(product.getVersion(), version);
        products.delete(product);
    }

    private static RecordNotFoundException missing() {
        return new RecordNotFoundException("Изделие не найдено или уже удалено. Обновите страницу");
    }

    private static void checkVersion(long actual, Long expected) {
        if (expected == null || actual != expected) {
            throw new BusinessException("Данные изменены в другой вкладке. Обновите страницу и повторите действие");
        }
    }
}
