package com.pratik.urlshortener.dto;

import lombok.Data;

@Data
public class UpdateExpiryRequest {

    private Integer expiryInDays;
}