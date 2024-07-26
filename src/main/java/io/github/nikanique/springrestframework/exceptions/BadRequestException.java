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
public class BadRequestException extends BaseException {
    private Map<String, String> errors;

    public BadRequestException(Map<String, String> errors) {
        super("Bad Request error");
        this.errors = errors;
    }

    public BadRequestException(String key, String errorMessage) {
        super("Bad Request error");
        errors = new HashMap<>();
        this.errors.put(key, errorMessage);
    }

    public BadRequestException(String message) {
        super(message);

    }

}
