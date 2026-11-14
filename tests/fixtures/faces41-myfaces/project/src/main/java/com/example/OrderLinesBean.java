package com.example;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@RequestScoped
public class OrderLinesBean {

    @Inject
    private LineService lineService;

    private boolean includeBackorders;
    private List<Line> lines;

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        lines = lineService.listOpenOrderLines(includeBackorders);
    }

    public void save() {
        lineService.update(lines);
    }

    public List<Line> getLines() {
        return lines;
    }

    public boolean isIncludeBackorders() {
        return includeBackorders;
    }

    public void setIncludeBackorders(boolean includeBackorders) {
        this.includeBackorders = includeBackorders;
    }

}
