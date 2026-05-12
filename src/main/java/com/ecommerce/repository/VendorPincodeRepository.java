package com.ecommerce.repository;

import com.ecommerce.entity.User;
import com.ecommerce.entity.VendorPincode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorPincodeRepository extends JpaRepository<VendorPincode, Long> {
    List<VendorPincode> findByVendorId(Long vendorId);
    boolean existsByVendorAndPincode(User vendor, String pincode);
}
