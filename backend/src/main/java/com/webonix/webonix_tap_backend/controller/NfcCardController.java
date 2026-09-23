package com.webonix.webonix_tap_backend.controller;

import com.webonix.webonix_tap_backend.dto.CreateNfcCardRequest;
import com.webonix.webonix_tap_backend.dto.NfcCardResponse;
import com.webonix.webonix_tap_backend.service.NfcCardService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class NfcCardController {

    private final NfcCardService nfcCardService;

    public NfcCardController(NfcCardService nfcCardService) {
        this.nfcCardService = nfcCardService;
    }

    @GetMapping
    public List<NfcCardResponse> getCards() {
        return nfcCardService.getAllCards();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NfcCardResponse createCard(
            @RequestBody CreateNfcCardRequest request
    ) {
        return nfcCardService.createCard(request);
    }
}