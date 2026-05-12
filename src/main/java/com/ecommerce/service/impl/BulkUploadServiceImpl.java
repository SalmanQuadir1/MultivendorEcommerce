package com.ecommerce.service.impl;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.User;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.BulkUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BulkUploadServiceImpl implements BulkUploadService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public Map<String, Object> uploadProductsFromCSV(MultipartFile file, Long vendorId) throws IOException {
        List<String[]> csvData = parseCSV(file);
        List<Map<String, String>> validatedData = validateCSVData(csvData);

        List<Map<String, String>> validRows = validatedData.stream()
                .filter(row -> "valid".equals(row.get("status")))
                .collect(Collectors.toList());

        List<Map<String, String>> invalidRows = validatedData.stream()
                .filter(row -> !"valid".equals(row.get("status")))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("totalRows", csvData.size());
        result.put("validRows", validRows.size());
        result.put("invalidRows", invalidRows.size());
        result.put("invalidData", invalidRows);

        if (!validRows.isEmpty()) {
            Map<String, Object> processingResult = processValidProducts(validRows, vendorId);
            result.putAll(processingResult);
        }

        return result;
    }

    @Override
    public byte[] generateProductTemplate() throws IOException {
        String csvContent = "name,description,price,category,imageUrl,size,sku,stock\n" +
                "Organic California Almonds,Premium raw California almonds rich in fiber and antioxidants,899.00,Dry Fruits,https://images.unsplash.com/photo-1596560548464-f01068e60207,500g,ALM001,120\n" +
                "Ayurvedic Herbal Face Serum,100% natural organic glow serum for youthful and radiant skin,1249.00,Cosmetics,https://images.unsplash.com/photo-1608248597481-496100c8c836,30ml,SER001,80\n" +
                "Premium Kashmiri Saffron,Highest grade pure organic saffron threads direct from Kashmir,399.00,Saffron,https://images.unsplash.com/photo-1509319117193-57bab727e09d,1g,SAF001,40\n";

        return csvContent.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public List<Map<String, String>> validateCSVData(List<String[]> csvData) {
        List<Map<String, String>> validatedRows = new ArrayList<>();
        List<String> existingCategories = categoryRepository.findAll().stream()
                .map(Category::getName)
                .collect(Collectors.toList());

        for (int i = 1; i < csvData.size(); i++) { // Skip header
            String[] row = csvData.get(i);
            if (row.length == 0) continue; // Skip empty rows

            Map<String, String> validatedRow = new HashMap<>();

            validatedRow.put("rowNumber", String.valueOf(i + 1));
            validatedRow.put("name", row.length > 0 ? row[0].trim() : "");
            validatedRow.put("description", row.length > 1 ? row[1].trim() : "");
            validatedRow.put("price", row.length > 2 ? row[2].trim() : "");
            validatedRow.put("category", row.length > 3 ? row[3].trim() : "");
            validatedRow.put("imageUrl", row.length > 4 ? row[4].trim() : "");
            validatedRow.put("size", row.length > 5 ? row[5].trim() : "");
            validatedRow.put("sku", row.length > 6 ? row[6].trim() : "");
            validatedRow.put("stock", row.length > 7 ? row[7].trim() : "");

            List<String> errors = new ArrayList<>();

            // Validate name
            if (validatedRow.get("name").isEmpty()) {
                errors.add("Product name is required");
            }

            // Validate price
            try {
                BigDecimal price = new BigDecimal(validatedRow.get("price"));
                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Price must be greater than 0");
                }
            } catch (NumberFormatException e) {
                errors.add("Invalid price format");
            }

            // Validate category
            if (!validatedRow.get("category").isEmpty() &&
                !existingCategories.contains(validatedRow.get("category"))) {
                errors.add("Category does not exist: " + validatedRow.get("category"));
            }

            // Validate stock
            try {
                Integer stock = Integer.parseInt(validatedRow.get("stock"));
                if (stock < 0) {
                    errors.add("Stock cannot be negative");
                }
            } catch (NumberFormatException e) {
                errors.add("Invalid stock format");
            }

            // Validate SKU uniqueness
            if (!validatedRow.get("sku").isEmpty()) {
                boolean skuExists = productVariantRepository.findAll().stream()
                        .anyMatch(v -> validatedRow.get("sku").equals(v.getSku()));
                if (skuExists) {
                    errors.add("SKU already exists: " + validatedRow.get("sku"));
                }
            }

            validatedRow.put("status", errors.isEmpty() ? "valid" : "invalid");
            validatedRow.put("errors", String.join("; ", errors));

            validatedRows.add(validatedRow);
        }

        return validatedRows;
    }

    @Override
    public Map<String, Object> processValidProducts(List<Map<String, String>> validData, Long vendorId) {
        User vendor = userRepository.findById(vendorId).orElse(null);
        if (vendor == null) {
            throw new IllegalArgumentException("Vendor not found");
        }

        List<Product> createdProducts = new ArrayList<>();
        List<ProductVariant> createdVariants = new ArrayList<>();
        int processedCount = 0;

        // Group by product name to handle multiple variants
        Map<String, List<Map<String, String>>> productsByName = validData.stream()
                .collect(Collectors.groupingBy(row -> row.get("name")));

        for (Map.Entry<String, List<Map<String, String>>> entry : productsByName.entrySet()) {
            List<Map<String, String>> productRows = entry.getValue();
            Map<String, String> firstRow = productRows.get(0);

            try {
                // Create product
                Product product = new Product();
                product.setName(firstRow.get("name"));
                product.setDescription(firstRow.get("description"));
                product.setPrice(new BigDecimal(firstRow.get("price")));
                product.setImageUrl(firstRow.get("imageUrl"));
                product.setVendor(vendor);

                // Set category if provided
                if (!firstRow.get("category").trim().isEmpty()) {
                    Category category = categoryRepository.findAll().stream()
                            .filter(c -> c.getName().equals(firstRow.get("category")))
                            .findFirst().orElse(null);
                    product.setCategory(category);
                }

                Product savedProduct = productRepository.save(product);
                createdProducts.add(savedProduct);

                // Create variants
                for (Map<String, String> row : productRows) {
                    ProductVariant variant = new ProductVariant();
                    variant.setProduct(savedProduct);
                    variant.setSize(row.get("size"));
                    variant.setSku(row.get("sku"));
                    variant.setStock(Integer.parseInt(row.get("stock")));
                    variant.setPrice(new BigDecimal(row.get("price")));
                    variant.setUnitPrice(new BigDecimal(row.get("price")).multiply(new BigDecimal("0.70")));

                    ProductVariant savedVariant = productVariantRepository.save(variant);
                    createdVariants.add(savedVariant);
                }

                processedCount += productRows.size();

            } catch (Exception e) {
                // Log error but continue with other products
                System.err.println("Error processing product " + firstRow.get("name") + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("processedCount", processedCount);
        result.put("createdProducts", createdProducts.size());
        result.put("createdVariants", createdVariants.size());
        result.put("products", createdProducts);

        return result;
    }

    private List<String[]> parseCSV(MultipartFile file) throws IOException {
        List<String[]> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) continue;

                // Parse CSV line manually (simple approach)
                List<String> fields = parseCSVLine(line);
                records.add(fields.toArray(new String[0]));
            }
        }

        return records;
    }

    private List<String> parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    currentField.append('"');
                    i++; // Skip next quote
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // Field separator
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }

        // Add the last field
        fields.add(currentField.toString());

        return fields;
    }
}