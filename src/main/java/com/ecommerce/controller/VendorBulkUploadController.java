package com.ecommerce.controller;

import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.BulkUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/vendor")
@RequiredArgsConstructor
public class VendorBulkUploadController {

    private final BulkUploadService bulkUploadService;

    @GetMapping("/bulk-upload")
    public String showBulkUploadPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("vendorId", userDetails.getId());
        return "vendor/bulk-upload";
    }

    @GetMapping("/bulk-upload/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] templateData = bulkUploadService.generateProductTemplate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=product_template.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(templateData);
    }

    @PostMapping("/bulk-upload/process")
    public String processBulkUpload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a CSV file to upload.");
            return "redirect:/vendor/bulk-upload";
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            redirectAttributes.addFlashAttribute("error", "Only CSV files are allowed.");
            return "redirect:/vendor/bulk-upload";
        }

        try {
            Map<String, Object> result = bulkUploadService.uploadProductsFromCSV(file, userDetails.getId());

            int totalRows = (Integer) result.get("totalRows");
            int validRows = (Integer) result.get("validRows");
            int invalidRows = (Integer) result.get("invalidRows");
            int processedCount = result.containsKey("processedCount") ? (Integer) result.get("processedCount") : 0;

            String successMessage = String.format(
                    "Upload completed! Total rows: %d, Valid: %d, Invalid: %d, Successfully processed: %d",
                    totalRows, validRows, invalidRows, processedCount
            );

            redirectAttributes.addFlashAttribute("success", successMessage);
            redirectAttributes.addFlashAttribute("uploadResult", result);

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to process the uploaded file: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
        }

        return "redirect:/vendor/bulk-upload";
    }
}