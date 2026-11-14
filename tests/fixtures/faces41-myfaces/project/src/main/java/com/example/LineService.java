package com.example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LineService {

    public List<Line> listOpenOrderLines(boolean includeBackorders) {
        List<Line> lines = new ArrayList<>();
        lines.add(new Line("a", "Keyboard", new BigDecimal("49.95"), 1));
        lines.add(new Line("b", "Mouse", new BigDecimal("24.95"), 2));

        if (includeBackorders) {
            lines.add(new Line("c", "Monitor", new BigDecimal("199.00"), 1));
        }

        return lines;
    }

    public List<Line> listBasketLines() {
        return listOpenOrderLines(false);
    }

    public void update(List<Line> lines) {
        // Persists the lines.
    }

}
