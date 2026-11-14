package com.example;

import java.io.Serializable;
import java.math.BigDecimal;

public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private BigDecimal price;

    public Product(String id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

}
