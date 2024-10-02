package io.github.nikanique.springrestframework.web.responses;

import lombok.Data;

import java.util.List;

@Data
public class PagedResponse<T> {
    public List<T> result;
    public Long totalCount;
    public String message;

    public PagedResponse(List<T> result, long totalCount) {
        this.result = result;
        this.totalCount = totalCount;
        this.message = "";
    }

    public PagedResponse(List<T> result, long totalCount, String message) {
        this.result = result;
        this.totalCount = totalCount;
        this.message = message;
    }

    public PagedResponse(String message) {
        this.message = message;
    }
}