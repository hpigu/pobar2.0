package com.pobar.dto.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class ProductSaveRequest {

    @NotBlank(message = "中文名稱不得為空")
    private String nameZh;

    private String nameEn;

    @NotNull(message = "分類不得為空")
    private Integer categoryId;

    @NotNull(message = "價格不得為空")
    @Positive(message = "價格必須大於 0")
    private BigDecimal price;

    @NotBlank(message = "類型不得為空")
    private String type;

    private String descriptionZh;
    private String descriptionEn;
    private LocalTime availableStartTime;
    private LocalTime availableEndTime;
    private LocalDate availableFromDate;
    private LocalDate availableToDate;

}
