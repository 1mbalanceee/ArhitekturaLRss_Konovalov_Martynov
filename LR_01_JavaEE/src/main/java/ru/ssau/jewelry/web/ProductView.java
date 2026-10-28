package ru.ssau.jewelry.web;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.List;
import ru.ssau.jewelry.model.Category;
import ru.ssau.jewelry.model.Product;
import ru.ssau.jewelry.service.InventoryService;

@Named
@ViewScoped
public class ProductView extends BaseView {
    private static final long serialVersionUID = 1L;

    @EJB
    private InventoryService service;

    private List<Product> products;
    private List<Category> categories;
    private Product selected = new Product();
    private Long categoryId;

    @PostConstruct
    public void init() {
        products = service.findProducts();
        categories = service.findCategories();
    }

    public void edit(Product product) {
        resetEditor();
        selected = new Product();
        selected.setId(product.getId());
        selected.setVersion(product.getVersion());
        selected.setSku(product.getSku());
        selected.setName(product.getName());
        selected.setMetal(product.getMetal());
        selected.setFineness(product.getFineness());
        selected.setWeightGrams(product.getWeightGrams());
        selected.setPrice(product.getPrice());
        selected.setStock(product.getStock());
        categoryId = product.getCategory().getId();
    }

    public void cancel() {
        resetEditor();
        selected = new Product();
        categoryId = null;
    }

    public void save() {
        if (execute(() -> service.saveProduct(selected, categoryId), "Изделие сохранено")) {
            cancel();
            init();
        }
    }

    public void delete(Product product) {
        if (execute(() -> service.deleteProduct(product.getId(), product.getVersion()), "Изделие удалено")) {
            cancel();
            init();
        }
    }

    public List<Product> getProducts() { return products; }
    public List<Category> getCategories() { return categories; }
    public Product getSelected() { return selected; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
