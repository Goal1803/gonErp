package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.model.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {
    boolean existsByName(String name);
}
