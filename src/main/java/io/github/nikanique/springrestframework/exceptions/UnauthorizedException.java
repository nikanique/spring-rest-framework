package io.github.nikanique.springrestframework.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
