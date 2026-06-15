package com.pratik.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.servlet.view.RedirectView;

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
    public ResponseEntity<Map<String, String>> handleCustomAliasAlreadyExistsException(
            CustomAliasAlreadyExistsException ex
    ) {

        Map<String, String> response = new HashMap<>();

        response.put("message", ex.getMessage());

        return new ResponseEntity<>(
                response,
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(UrlExpiredException.class)

    public RedirectView handleURLExpireException(

            UrlExpiredException ex

    ) {

        return new RedirectView(
                "https://link-zen.vercel.app/expired-link"
        );

    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(
            RuntimeException ex
    ) {

        Map<String, String> response = new HashMap<>();

        response.put("message", ex.getMessage());

        return new ResponseEntity<>(
                response,
                HttpStatus.BAD_REQUEST
        );
    }

}