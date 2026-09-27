package com.example;

import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ViewScoped
public class OrdersBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private OrderService orderService;

    private String customer;
    private String reference;
    private String deliveryCity;
    private String summary;
    private List<Order> orders;

    @PostConstruct
    public void init() {
        orders = orderService.list(customer);
    }

    public void refresh() {
        orders = orderService.list(customer);
    }

    public void select(Order order) {
        summary = order.getNumber();
    }

    public void save() {
        orderService.save(reference);
    }

    public void export() {
        orderService.exportToPdf(orders);
    }

    public List<Order> getOrders() {
        return orders;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDeliveryCity() {
        return deliveryCity;
    }

    public void setDeliveryCity(String deliveryCity) {
        this.deliveryCity = deliveryCity;
    }

    public String getSummary() {
        return summary;
    }

}
