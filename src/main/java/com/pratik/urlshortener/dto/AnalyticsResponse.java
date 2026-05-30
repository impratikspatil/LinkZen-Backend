package com.pratik.urlshortener.dto;

import com.pratik.urlshortener.model.UrlClick;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalyticsResponse {

    private Map<String, Long> browserStats;

    private Map<String, Long> deviceStats;

    private Map<String, Long> weeklyClicks;

    private List<UrlClick> recentActivities;

}
