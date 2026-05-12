package com.ecommerce.repository;

import com.ecommerce.entity.CarouselItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CarouselItemRepository extends JpaRepository<CarouselItem, Long> {
    List<CarouselItem> findByActiveTrue();
}
