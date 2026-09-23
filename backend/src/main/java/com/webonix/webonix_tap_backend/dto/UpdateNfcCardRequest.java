package com.webonix.webonix_tap_backend.dto;

public record UpdateNfcCardRequest(
        String name,
        String type,
        String destinationUrl
) {
}