package com.webonix.webonix_tap_backend.dto;

public record CreateNfcCardRequest(
        String name,
        String type,
        String destinationUrl
) {
}