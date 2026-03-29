package com.twelvenfactoragentic.demotwelvenfactoragentic.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class Product {

    @Id
    private String id;

    private Integer quantity;

    public Product(){}

    public Product(String id, Integer quantity) {
        this.id = id;
        this.quantity = quantity;
    }

    // getters setters
}
