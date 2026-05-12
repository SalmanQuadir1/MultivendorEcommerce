package com.ecommerce.controller;

import com.ecommerce.entity.CarouselItem;
import com.ecommerce.repository.CarouselItemRepository;
import com.ecommerce.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/admin/carousel")
@RequiredArgsConstructor
public class AdminCarouselController {

    private final CarouselItemRepository carouselItemRepository;
    private final FileUploadService fileUploadService;

    @GetMapping
    public String listCarousel(Model model) {
        model.addAttribute("carouselItems", carouselItemRepository.findAll());
        model.addAttribute("newItem", new CarouselItem());
        return "admin/carousel/list";
    }

    @PostMapping("/save")
    public String saveCarouselItem(@ModelAttribute("newItem") CarouselItem carouselItem,
                                   @RequestParam("imageFile") MultipartFile imageFile) throws IOException {
        if (!imageFile.isEmpty()) {
            String imageUrl = fileUploadService.uploadFile(imageFile);
            carouselItem.setImageUrl(imageUrl);
        }
        carouselItem.setActive(true);
        carouselItemRepository.save(carouselItem);
        return "redirect:/admin/carousel";
    }

    @PostMapping("/toggle/{id}")
    public String toggleCarouselItem(@PathVariable Long id) {
        CarouselItem item = carouselItemRepository.findById(id).orElse(null);
        if (item != null) {
            item.setActive(!item.isActive());
            carouselItemRepository.save(item);
        }
        return "redirect:/admin/carousel";
    }

    @GetMapping("/delete/{id}")
    public String deleteCarouselItem(@PathVariable Long id) {
        carouselItemRepository.deleteById(id);
        return "redirect:/admin/carousel";
    }
}
