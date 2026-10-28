package ru.ssau.jewelry.web;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.util.List;
import ru.ssau.jewelry.model.Category;
import ru.ssau.jewelry.service.InventoryService;

@Named
@ViewScoped
public class CategoryView extends BaseView {
    private static final long serialVersionUID = 1L;

    @EJB
    private InventoryService service;

    private List<Category> categories;
    private Category selected = new Category();

    @PostConstruct
    public void init() {
        categories = service.findCategories();
    }

    public void edit(Category category) {
        resetEditor();
        selected = new Category();
        selected.setId(category.getId());
        selected.setVersion(category.getVersion());
        selected.setName(category.getName());
        selected.setDescription(category.getDescription());
    }

    public void cancel() {
        resetEditor();
        selected = new Category();
    }

    public void save() {
        if (execute(() -> service.saveCategory(selected), "Категория сохранена")) {
            cancel();
            init();
        }
    }

    public void delete(Category category) {
        if (execute(() -> service.deleteCategory(category.getId(), category.getVersion()), "Категория удалена")) {
            cancel();
            init();
        }
    }

    public List<Category> getCategories() { return categories; }
    public Category getSelected() { return selected; }
}
