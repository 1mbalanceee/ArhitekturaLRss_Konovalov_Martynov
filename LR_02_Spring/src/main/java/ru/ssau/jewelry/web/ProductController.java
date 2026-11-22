package ru.ssau.jewelry.web;

import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ssau.jewelry.dto.DeleteForm;
import ru.ssau.jewelry.dto.ProductForm;
import ru.ssau.jewelry.exception.BusinessException;
import ru.ssau.jewelry.service.CategoryService;
import ru.ssau.jewelry.service.ProductService;

@Controller
@RequestMapping("/products")
public class ProductController {
    private final ProductService service;
    private final CategoryService categories;
    private final WebErrors errors;

    public ProductController(ProductService service, CategoryService categories, WebErrors errors) {
        this.service = service;
        this.categories = categories;
        this.errors = errors;
    }

    @InitBinder("form")
    public void allowedFields(WebDataBinder binder) {
        binder.setAllowedFields("id", "version", "sku", "name", "metal", "fineness",
                "weightGrams", "price", "stock", "categoryId");
    }

    @GetMapping
    public String list(Model model) {
        ProductForm form = new ProductForm();
        form.setVersion(0L);
        model.addAttribute("form", form);
        return page(model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("form", service.findForm(id));
        return page(model);
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") ProductForm form, BindingResult result,
                       Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                service.save(form);
                redirect.addFlashAttribute("success", "Изделие сохранено");
                return "redirect:/products";
            } catch (BusinessException exception) {
                errors.reject(result, exception);
            } catch (DataAccessException exception) {
                errors.reject(result, exception);
            }
        }
        return page(model);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @Valid @ModelAttribute("deleteForm") DeleteForm form,
                         BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", "Некорректная версия записи. Обновите страницу");
        } else {
            try {
                service.delete(id, form.getVersion());
                redirect.addFlashAttribute("success", "Изделие удалено");
            } catch (BusinessException exception) {
                redirect.addFlashAttribute("error", exception.getMessage());
            } catch (DataAccessException exception) {
                redirect.addFlashAttribute("error", errors.message(exception));
            }
        }
        return "redirect:/products";
    }

    private String page(Model model) {
        model.addAttribute("products", service.findAll());
        model.addAttribute("categories", categories.findAll());
        return "products";
    }
}
