package io.github.nikanique.springrestframework.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

@ResponseStatus(HttpStatus.BAD_REQUEST)
@Getter
@Setter
public class ValidationException extends BaseException {
    private Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super("Validation error");
        this.errors = errors;
    }

    public ValidationException(String key, String errorMessage) {
        super("Validation error");
        errors = new HashMap<>();
        this.errors.put(key, errorMessage);
    }

}
