package com.pobar.common;

import lombok.Getter;

import java.util.List;

@Getter
public class PageResult<T> {

    private final long total;
    private final List<T> records;

    public PageResult(long total, List<T> records) {
        this.total = total;
        this.records = records;
    }
}
