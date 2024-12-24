package io.github.nikanique.springrestframework.advices;


import io.github.nikanique.springrestframework.exceptions.BadRequestException;
import io.github.nikanique.springrestframework.exceptions.BaseException;
import io.github.nikanique.springrestframework.exceptions.UnauthorizedException;
import io.github.nikanique.springrestframework.exceptions.ValidationException;
import io.github.nikanique.springrestframework.web.responses.ErrorResponse;
import io.github.nikanique.springrestframework.web.responses.ValidationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ValidationException.class, BadRequestException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(BaseException ex) {
        ValidationErrorResponse validationErrorResponse = new ValidationErrorResponse(ex.getMessage(), ex.getErrors());
        return new ResponseEntity<>(validationErrorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({UnauthorizedException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(BaseException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

}

