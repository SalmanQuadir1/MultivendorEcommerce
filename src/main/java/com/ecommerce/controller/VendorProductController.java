package com.ecommerce.controller;

import com.ecommerce.entity.Product;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.FileUploadService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/vendor/products")
@RequiredArgsConstructor
public class VendorProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final FileUploadService fileUploadService;
    private final UserService userService;

    @GetMapping
    public String listProducts(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("products", productRepository.findByVendorIdWithVariants(userDetails.getId()));
        return "vendor/products/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.findAll());
        return "vendor/products/form";
    }

    @PostMapping("/save")
    public String saveProduct(@ModelAttribute("product") Product product,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam("variantSizes") java.util.List<String> sizes,
            @RequestParam("variantStocks") java.util.List<Integer> stocks,
            @RequestParam("variantUnitPrices") java.util.List<java.math.BigDecimal> unitPrices,
            @RequestParam("variantPrices") java.util.List<java.math.BigDecimal> prices,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        if (!imageFile.isEmpty()) {
            String imageUrl = fileUploadService.uploadFile(imageFile);
            product.setImageUrl(imageUrl);
        } else if (product.getId() != null) {
            Product existingProduct = productService.findById(product.getId());
            product.setImageUrl(existingProduct.getImageUrl());
        }

        product.setVendor(userService.findByEmail(userDetails.getUsername()));

        // Ensure product price is set from first variant when readonly
        if (!sizes.isEmpty() && !prices.isEmpty() && prices.get(0) != null) {
            product.setPrice(prices.get(0));
        }

        // Save base product first so it gets an ID (if new)
        Product savedProduct = productService.save(product);

        savedProduct.getVariants().clear();

        // Add variants
        for (int i = 0; i < sizes.size(); i++) {
            if (sizes.get(i) != null && !sizes.get(i).trim().isEmpty()) {
                com.ecommerce.entity.ProductVariant v = new com.ecommerce.entity.ProductVariant();
                v.setProduct(savedProduct);
                v.setSize(sizes.get(i));
                v.setStock(stocks.get(i) != null ? stocks.get(i) : 0);
                java.math.BigDecimal vPrice = prices.get(i) != null ? prices.get(i) : savedProduct.getPrice();
                java.math.BigDecimal vUnitPrice = unitPrices.get(i) != null ? unitPrices.get(i) : java.math.BigDecimal.ZERO;
                v.setPrice(vPrice);
                v.setUnitPrice(vUnitPrice);
                v.setSku(savedProduct.getName().substring(0, Math.min(3, savedProduct.getName().length())).toUpperCase()
                        + "-" + sizes.get(i).toUpperCase());
                savedProduct.getVariants().add(v);
            }
        }

        productService.save(savedProduct);

        return "redirect:/vendor/products";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        Product product = productService.findById(id);
        if (product != null && product.getVendor().getId().equals(userDetails.getId())) {
            model.addAttribute("product", product);
            model.addAttribute("categories", categoryService.findAll());
            return "vendor/products/form";
        }
        return "redirect:/vendor/products";
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Product product = productService.findById(id);
        if (product != null && product.getVendor().getId().equals(userDetails.getId())) {
            productService.deleteById(id);
        }
        return "redirect:/vendor/products";
    }
}
