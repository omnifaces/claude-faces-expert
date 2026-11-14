package com.example;

import java.io.Serializable;
import java.math.BigDecimal;

public class Line implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String product;
    private final BigDecimal price;
    private Integer quantity;
    private boolean giftWrap;

    public Line(String id, String product, BigDecimal price, Integer quantity) {
        this.id = id;
        this.product = product;
        this.price = price;
        this.quantity = quantity;
    }

    public String getId() {
        return id;
    }

    public String getProduct() {
        return product;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public boolean isGiftWrap() {
        return giftWrap;
    }

    public void setGiftWrap(boolean giftWrap) {
        this.giftWrap = giftWrap;
    }

}
