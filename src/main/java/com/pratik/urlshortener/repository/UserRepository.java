package com.pratik.urlshortener.repository;

import com.pratik.urlshortener.model.User;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/*
 * Repository layer for User collection.
 */
@Repository
public interface UserRepository
        extends MongoRepository<User, String> {

    /*
     * Find user using email.
     */
    Optional<User> findByEmail(String email);

    /*
     * Check whether email already exists.
     */
    boolean existsByEmail(String email);
}