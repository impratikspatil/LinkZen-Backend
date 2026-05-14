package com.pratik.urlshortener.exception;

/*
 * Thrown when custom alias already exists.
 */
public class CustomAliasAlreadyExistsException
        extends RuntimeException {

    public CustomAliasAlreadyExistsException(
            String message
    ) {

        super(message);
    }
}