package io.github.nikanique.springrestframework.web.responses;

import lombok.Data;

import java.util.Map;

@Data
public class ValidationErrorResponse extends ErrorResponse {
    private Map<String, String> fields;

    public ValidationErrorResponse(String message, Map<String, String> fields) {
        super(message);
        this.fields = fields;
    }
}