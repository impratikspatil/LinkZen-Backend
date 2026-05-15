package com.pratik.urlshortener.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    /*
     * Full name of user.
     */
    private String name;

    /*
     * User email.
     * Used for login.
     */
    private String email;

    /*
     * Encrypted password.
     */
    private String password;
}