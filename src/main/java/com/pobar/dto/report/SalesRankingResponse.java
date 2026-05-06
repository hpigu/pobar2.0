package com.pobar.dto.report;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SalesRankingResponse {

    private List<ProductRank> products;

    @Data
    public static class ProductRank {
        private Integer productId;
        private String productName;
        private String categoryName;
        private Integer totalQuantity;
        private BigDecimal totalRevenue;
    }
}
