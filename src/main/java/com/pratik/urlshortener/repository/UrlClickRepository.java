package com.pratik.urlshortener.repository;

import com.pratik.urlshortener.model.UrlClick;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UrlClickRepository
        extends MongoRepository<UrlClick, String> {
}