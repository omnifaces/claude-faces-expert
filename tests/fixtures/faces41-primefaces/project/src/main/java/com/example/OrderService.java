package com.example;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderService {

    public List<Order> list(String customer) {
        return List.of();
    }

    public void save(String reference) {
        // Persists the order.
    }

    public void attach(String fileName, byte[] content) {
        // Stores the attachment.
    }

    public void exportToPdf(List<Order> orders) {
        // Streams a PDF to the response.
    }

}
