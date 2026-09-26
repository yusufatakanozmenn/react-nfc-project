package com.webonix.webonix_tap_backend.controller;

import com.webonix.webonix_tap_backend.dto.*;
import com.webonix.webonix_tap_backend.service.NfcCardService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class NfcCardController {
    private final NfcCardService cards;
    public NfcCardController(NfcCardService cards) { this.cards = cards; }

    @GetMapping
    public List<NfcCardResponse> getCards(Principal principal) { return cards.getAllCards(principal.getName()); }
    @GetMapping("/{id}")
    public NfcCardResponse getCard(@PathVariable Long id, Principal principal) {
        return cards.getCardById(id, principal.getName());
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NfcCardResponse createCard(@RequestBody CreateNfcCardRequest request, Principal principal) {
        return cards.createCard(request, principal.getName());
    }
    @PutMapping("/{id}")
    public NfcCardResponse updateCard(@PathVariable Long id, @RequestBody UpdateNfcCardRequest request, Principal principal) {
        return cards.updateCard(id, request, principal.getName());
    }
    @PatchMapping("/{id}/status")
    public NfcCardResponse toggleStatus(@PathVariable Long id, Principal principal) {
        return cards.toggleStatus(id, principal.getName());
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable Long id, Principal principal) { cards.deleteCard(id, principal.getName()); }
    @PutMapping("/{id}/owner")
    public NfcCardResponse assignOwner(@PathVariable Long id, @RequestBody AssignCardOwnerRequest request, Principal principal) {
        return cards.assignOwner(id, request.ownerId(), principal.getName());
    }
}
