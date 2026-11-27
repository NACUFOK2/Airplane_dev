package com.airplane.dto;

public record SourceResponse(int index, String airlineCode, String airlineName, String category, String title, String snippet, double score) {}
