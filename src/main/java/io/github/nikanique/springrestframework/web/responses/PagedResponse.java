package io.github.nikanique.springrestframework.web.responses;

import lombok.Data;

import java.util.List;

@Data
public class PagedResponse<T> {
    public List<T> result;
    public Long totalCount;

    public PagedResponse(List<T> result, long totalCount) {
        this.result = result;
        this.totalCount = totalCount;
    }
}