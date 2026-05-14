package com.pratik.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

/*
 * Handles application-wide exceptions.
 */
@ControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String, String> handleValidationExceptions(
            MethodArgumentNotValidException ex
    ) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return errors;
    }

    @ExceptionHandler(CustomAliasAlreadyExistsException.class)
    public ResponseEntity<String> handleCustomAliasAlreadyExistsException(
            CustomAliasAlreadyExistsException ex
    ) {
        return new ResponseEntity<>(
                ex.getMessage(),
                HttpStatus.CONFLICT
        );

    }

    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<String> handleURLExpireException(
            UrlExpiredException ex
    ) {
        return new ResponseEntity<>(
                ex.getMessage(),
                HttpStatus.CONFLICT
        );

    }

}