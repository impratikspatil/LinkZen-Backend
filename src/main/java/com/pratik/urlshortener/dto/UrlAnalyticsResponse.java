package com.pratik.urlshortener.dto;

import com.pratik.urlshortener.model.UrlClick;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class UrlAnalyticsResponse {

    private String shortCode;

    private String originalUrl;

    private Long clickCount;

    private String createdAt;

    private String expiresAt;

    private Map<String, Long> browserStats;

    private Map<String, Long> deviceStats;

    private List<UrlClick> recentActivities;
}