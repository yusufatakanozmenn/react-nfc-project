package com.webonix.webonix_tap_backend.controller;

import com.webonix.webonix_tap_backend.dto.CreateNfcCardRequest;
import com.webonix.webonix_tap_backend.dto.NfcCardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
public class NfcCardController {

    private final List<NfcCardResponse> cards = new ArrayList<>();

    public NfcCardController() {

        cards.add(new NfcCardResponse(
                1L,
                "Google Yorum Kartı",
                "google",
                "A8K2XP",
                "https://google.com",
                128,
                true
        ));

        cards.add(new NfcCardResponse(
                2L,
                "Instagram Kartı",
                "instagram",
                "B7X92M",
                "https://instagram.com",
                54,
                true
        ));
    }

    @GetMapping
    public List<NfcCardResponse> getCards() {
        return cards;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NfcCardResponse createCard(
            @RequestBody CreateNfcCardRequest request
    ) {

        String code = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();

        Long id = (long) (cards.size() + 1);

        NfcCardResponse newCard = new NfcCardResponse(
                id,
                request.name(),
                request.type(),
                code,
                request.destinationUrl(),
                0,
                true
        );

        cards.add(newCard);

        return newCard;
    }
}