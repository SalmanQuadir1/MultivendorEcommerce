package com.ecommerce.controller;

import com.ecommerce.entity.Category;
import com.ecommerce.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vendor/categories")
@RequiredArgsConstructor
public class VendorCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("newCategory", new Category());
        return "vendor/categories/list";
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute("newCategory") Category category, RedirectAttributes ra) {
        try {
            categoryService.save(category);
            ra.addFlashAttribute("success", "Category \"" + category.getName() + "\" created successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to save category: " + e.getMessage());
        }
        return "redirect:/vendor/categories";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Category cat = categoryService.findById(id);
            String name = cat != null ? cat.getName() : "Unknown";
            categoryService.deleteById(id);
            ra.addFlashAttribute("success", "Category \"" + name + "\" deleted successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete category: " + e.getMessage());
        }
        return "redirect:/vendor/categories";
    }
}
