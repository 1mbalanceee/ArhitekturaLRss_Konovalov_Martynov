package ru.ssau.jewelry.web;

import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ssau.jewelry.dto.CategoryForm;
import ru.ssau.jewelry.dto.DeleteForm;
import ru.ssau.jewelry.exception.BusinessException;
import ru.ssau.jewelry.service.CategoryService;

@Controller
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService service;
    private final WebErrors errors;

    public CategoryController(CategoryService service, WebErrors errors) {
        this.service = service;
        this.errors = errors;
    }

    @InitBinder("form")
    public void allowedFields(WebDataBinder binder) {
        binder.setAllowedFields("id", "version", "name", "description");
    }

    @GetMapping
    public String list(Model model) {
        CategoryForm form = new CategoryForm();
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
    public String save(@Valid @ModelAttribute("form") CategoryForm form, BindingResult result,
                       Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                service.save(form);
                redirect.addFlashAttribute("success", "Категория сохранена");
                return "redirect:/categories";
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
                redirect.addFlashAttribute("success", "Категория удалена");
            } catch (BusinessException exception) {
                redirect.addFlashAttribute("error", exception.getMessage());
            } catch (DataAccessException exception) {
                redirect.addFlashAttribute("error", errors.message(exception));
            }
        }
        return "redirect:/categories";
    }

    private String page(Model model) {
        model.addAttribute("categories", service.findAll());
        return "categories";
    }
}
