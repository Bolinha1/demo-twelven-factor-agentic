package com.twelvenfactoragentic.demotwelvenfactoragentic.service;

import com.twelvenfactoragentic.demotwelvenfactoragentic.model.Product;
import com.twelvenfactoragentic.demotwelvenfactoragentic.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryService {

    private final ProductRepository repository;

    public InventoryService(ProductRepository repository) {
        this.repository = repository;
    }


    public void stockIn(String productId, int quantity) {

        Product product = repository
                .findById(productId)
                .orElse(new Product(productId,0));

        product.setQuantity(product.getQuantity()+quantity);

        repository.save(product);
    }

    public List<Product> listProducts() {
        return repository.findAll();
    }

    public void stockOut(String productId, int quantity) {

        Product product = repository
                .findById(productId)
                .orElseThrow();

        product.setQuantity(product.getQuantity()-quantity);

        repository.save(product);
    }
}