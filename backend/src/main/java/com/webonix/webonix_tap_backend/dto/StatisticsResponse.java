package com.webonix.webonix_tap_backend.dto;
import java.util.List;
public record StatisticsResponse(long totalCards, long activeCards, long inactiveCards,
                                 long totalScans, List<NfcCardResponse> cards) {}
