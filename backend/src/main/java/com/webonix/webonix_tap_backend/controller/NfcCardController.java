package com.webonix.webonix_tap_backend.controller;

import com.webonix.webonix_tap_backend.dto.NfcCardResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class NfcCardController {

    @GetMapping
    public List<NfcCardResponse> getCards() {

        NfcCardResponse card1 = new NfcCardResponse(
                1L,
                "Google Yorum Kartı",
                "google",
                "A8K2XP",
                "https://google.com",
                128,
                true
        );

        NfcCardResponse card2 = new NfcCardResponse(
                2L,
                "Instagram Kartı",
                "instagram",
                "B7X92M",
                "https://instagram.com",
                54,
                true
        );

        return List.of(card1, card2);
    }
}