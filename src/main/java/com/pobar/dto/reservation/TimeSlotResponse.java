package com.pobar.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeSlotResponse {
    private String time;       // "17:00"
    private boolean available;
    private int reservedCount;
    private int totalTables;
}
