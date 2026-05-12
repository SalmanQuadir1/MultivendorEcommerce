package com.ecommerce.service;

import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface BulkUploadService {
    Map<String, Object> uploadProductsFromCSV(MultipartFile file, Long vendorId) throws IOException;
    byte[] generateProductTemplate() throws IOException;
    List<Map<String, String>> validateCSVData(List<String[]> csvData);
    Map<String, Object> processValidProducts(List<Map<String, String>> validData, Long vendorId);
}