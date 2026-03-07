package com.twelvenfactoragentic.demotwelvenfactoragentic.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.twelvenfactoragentic.demotwelvenfactoragentic.model.Product;

@Repository
public interface ProductRepository
        extends JpaRepository<Product,String> {
}