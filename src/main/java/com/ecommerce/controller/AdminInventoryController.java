package com.ecommerce.controller;

import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @GetMapping
    public String listInventory(Model model) {
        List<Product> products = productRepository.findAllWithVariants();
        model.addAttribute("products", products);
        return "admin/inventory/list";
    }

    @GetMapping("/edit/{id}")
    public String showEditStockForm(@PathVariable("id") Long id, Model model) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            model.addAttribute("product", product);
            return "admin/inventory/edit";
        }
        return "redirect:/admin/inventory";
    }

    @PostMapping("/update/{id}")
    public String updateStock(@PathVariable("id") Long variantId, @RequestParam("stock") Integer stock) {
        ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
        if (variant != null) {
            variant.setStock(stock);
            productVariantRepository.save(variant);
        }
        return "redirect:/admin/inventory";
    }

    @GetMapping("/low-stock")
    public String listLowStock(Model model) {
        List<Product> allProducts = productRepository.findAllWithVariants();
        List<Product> lowStockProducts = allProducts.stream()
                .filter(product -> product.getVariants().stream().anyMatch(v -> v.getStock() != null && v.getStock() <= 10))
                .collect(Collectors.toList());
        model.addAttribute("products", lowStockProducts);
        return "admin/inventory/low-stock";
    }
}