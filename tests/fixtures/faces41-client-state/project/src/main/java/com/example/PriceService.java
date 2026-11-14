package com.example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PriceService {

    public List<Product> listProducts() {
        List<Product> products = new ArrayList<>();
        products.add(new Product("kb", "Keyboard", new BigDecimal("49.95")));
        products.add(new Product("ms", "Mouse", new BigDecimal("24.95")));
        return products;
    }

    public Map<String, BigDecimal> mapSurchargesByRegion() {
        Map<String, BigDecimal> surcharges = new LinkedHashMap<>();
        surcharges.put("EU", new BigDecimal("0.00"));
        surcharges.put("US", new BigDecimal("4.95"));
        return surcharges;
    }

    public void update(List<Product> products) {
        // Persists the prices.
    }

}
