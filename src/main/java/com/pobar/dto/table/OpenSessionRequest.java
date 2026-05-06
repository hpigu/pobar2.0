package com.pobar.dto.table;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OpenSessionRequest {

    @NotEmpty(message = "至少選一張桌子")
    private List<Integer> tableIds;

    @Min(value = 1, message = "人數至少 1 人")
    private int partySize;
}
