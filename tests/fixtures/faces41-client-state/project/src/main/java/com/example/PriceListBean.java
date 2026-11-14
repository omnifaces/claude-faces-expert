package com.example;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ViewScoped
public class PriceListBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private PriceService priceService;

    private List<Product> products;
    private Map<String, BigDecimal> surchargesByRegion;
    private BigDecimal total;

    @PostConstruct
    public void init() {
        products = priceService.listProducts();
        surchargesByRegion = priceService.mapSurchargesByRegion();
        recalculate();
    }

    public void recalculate() {
        BigDecimal sum = BigDecimal.ZERO;

        for (Product product : products) {
            if (product.getPrice() != null) {
                sum = sum.add(product.getPrice());
            }
        }

        total = sum;
    }

    public void save() {
        priceService.update(products);
    }

    public List<Product> getProducts() {
        return products;
    }

    public Map<String, BigDecimal> getSurchargesByRegion() {
        return surchargesByRegion;
    }

    public BigDecimal getTotal() {
        return total;
    }

}
