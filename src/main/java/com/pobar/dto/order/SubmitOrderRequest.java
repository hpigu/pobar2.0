package com.pobar.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SubmitOrderRequest {

    @NotEmpty(message = "訂單不得為空")
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {

        @NotNull(message = "品項 ID 不得為空")
        private Integer productId;

        @Min(value = 1, message = "數量至少 1")
        private int quantity;

        @Size(max = 100, message = "備註不得超過 100 字")
        private String notes;
    }
}
