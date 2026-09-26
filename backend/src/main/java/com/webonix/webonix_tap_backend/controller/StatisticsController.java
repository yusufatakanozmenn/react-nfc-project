package com.webonix.webonix_tap_backend.controller;
import com.webonix.webonix_tap_backend.dto.StatisticsResponse;
import com.webonix.webonix_tap_backend.service.NfcCardService;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    private final NfcCardService cards;
    public StatisticsController(NfcCardService cards) { this.cards = cards; }
    @GetMapping
    public StatisticsResponse getStatistics(Principal principal) { return cards.statistics(principal.getName()); }
}
