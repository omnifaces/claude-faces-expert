package com.example;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ViewScoped
public class BasketBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final BigDecimal GIFT_WRAP_FEE = new BigDecimal("2.50");

    @Inject
    private LineService lineService;

    private List<Line> lines;
    private BigDecimal total;

    @PostConstruct
    public void init() {
        lines = lineService.listBasketLines();
        recalculate();
    }

    public void recalculate() {
        BigDecimal sum = BigDecimal.ZERO;

        for (Line line : lines) {
            if (line.getQuantity() != null) {
                sum = sum.add(line.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
            }

            if (line.isGiftWrap()) {
                sum = sum.add(GIFT_WRAP_FEE);
            }
        }

        total = sum;
    }

    public void giftWrapChanged(AjaxBehaviorEvent event) {
        recalculate();
    }

    public void checkout() {
        lineService.update(lines);
    }

    public List<Line> getLines() {
        return lines;
    }

    public BigDecimal getTotal() {
        return total;
    }

}
