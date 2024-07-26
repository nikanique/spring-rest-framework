package io.github.nikanique.springrestframework.exceptions;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;


@Getter
@Setter
public class BaseException extends RuntimeException {
    private Map<String, String> errors;

    public BaseException(String message) {
        super(message);
    }

    public BaseException(Map<String, String> errors) {
        super("Error");
        this.errors = errors;
    }

    public BaseException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public BaseException(String message, String key, String errorMessage) {
        super(message);
        errors = new HashMap<>();
        this.errors.put(key, errorMessage);
    }

}
