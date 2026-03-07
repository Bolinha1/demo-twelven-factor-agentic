package com.twelvenfactoragentic.demotwelvenfactoragentic.service;

import com.twelvenfactoragentic.demotwelvenfactoragentic.model.Product;
import com.twelvenfactoragentic.demotwelvenfactoragentic.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private final ProductRepository repository;

    public InventoryService(ProductRepository repository) {
        this.repository = repository;
    }

    /**
     * Factor 5
     * Estado de negócio fora do agente
     */
    public void stockIn(String productId, int quantity) {

        Product product = repository
                .findById(productId)
                .orElse(new Product(productId,0));

        product.setQuantity(product.getQuantity()+quantity);

        repository.save(product);
    }

    public void stockOut(String productId, int quantity) {

        Product product = repository
                .findById(productId)
                .orElseThrow();

        product.setQuantity(product.getQuantity()-quantity);

        repository.save(product);
    }
}