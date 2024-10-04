package io.github.nikanique.springrestframework.web.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private Map<String, String> fields;

    public ErrorResponse(String message) {
        this.message = message;
    }
}