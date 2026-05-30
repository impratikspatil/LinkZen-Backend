package com.pratik.urlshortener.repository;

import com.pratik.urlshortener.model.UrlClick;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UrlClickRepository
        extends MongoRepository<UrlClick, String> {

    List<UrlClick> findByShortCode(String shortCode);

    List<UrlClick> findAllByOrderByClickedAtDesc();

    List<UrlClick> findByUserEmail(String userEmail);
}