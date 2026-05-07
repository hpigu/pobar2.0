package com.pobar.dto.menu;

import lombok.Data;

import java.util.List;

@Data
public class ProductQueryRequest {
    private String type;
    private Integer categoryId;
    private Boolean available;
    private Integer page = 1;
    private Integer size = 50;
}
