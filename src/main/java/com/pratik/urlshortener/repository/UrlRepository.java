package com.pratik.urlshortener.repository;

import com.pratik.urlshortener.model.Url;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/*
 * Repository layer for URL collection.
 * Handles database operations.
 */
@Repository
public interface UrlRepository extends MongoRepository<Url, String> {

    /*
     * Find URL document using shortCode.
     *
     * Example:
     * shortCode -> abc123
     */
    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Optional<Url> findByOriginalUrl(String originalUrl);

    List<Url> findAllByOrderByCreatedAtDesc();
}