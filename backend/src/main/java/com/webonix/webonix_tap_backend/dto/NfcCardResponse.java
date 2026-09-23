package com.webonix.webonix_tap_backend.dto;

public record NfcCardResponse(
        Long id,
        String name,
        String type,
        String code,
        String destinationUrl,
        Integer scans,
        Boolean active
) {
}