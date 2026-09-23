package com.webonix.webonix_tap_backend.service;

import com.webonix.webonix_tap_backend.dto.CreateNfcCardRequest;
import com.webonix.webonix_tap_backend.dto.NfcCardResponse;
import com.webonix.webonix_tap_backend.entity.NfcCard;
import com.webonix.webonix_tap_backend.repository.NfcCardRepository;
import org.springframework.stereotype.Service;
import com.webonix.webonix_tap_backend.dto.UpdateNfcCardRequest;

import java.util.List;
import java.util.UUID;

@Service
public class NfcCardService {

    private final NfcCardRepository nfcCardRepository;

    public NfcCardService(NfcCardRepository nfcCardRepository) {
        this.nfcCardRepository = nfcCardRepository;
    }

    public List<NfcCardResponse> getAllCards() {

        return nfcCardRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public NfcCardResponse createCard(CreateNfcCardRequest request) {

        String code = generateUniqueCode();

        NfcCard card = new NfcCard(
                request.name(),
                request.type(),
                code,
                request.destinationUrl(),
                0,
                true
        );

        NfcCard savedCard = nfcCardRepository.save(card);

        return toResponse(savedCard);
    }

    private String generateUniqueCode() {

        String code;

        do {
            code = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase();

        } while (nfcCardRepository.existsByCode(code));

        return code;
    }

    private NfcCardResponse toResponse(NfcCard card) {

        return new NfcCardResponse(
                card.getId(),
                card.getName(),
                card.getType(),
                card.getCode(),
                card.getDestinationUrl(),
                card.getScans(),
                card.getActive()
        );
    }

    public void deleteCard(Long id) {

    if (!nfcCardRepository.existsById(id)) {
        throw new RuntimeException("Kart bulunamadı");
    }

    nfcCardRepository.deleteById(id);
    }
    public NfcCardResponse toggleStatus(Long id) {

    NfcCard card = nfcCardRepository
            .findById(id)
            .orElseThrow(() ->
                    new RuntimeException("Kart bulunamadı")
            );

    card.setActive(!Boolean.TRUE.equals(card.getActive()));

    NfcCard updatedCard = nfcCardRepository.save(card);

    return toResponse(updatedCard);
    }
    public NfcCardResponse getCardById(Long id) {

    NfcCard card = nfcCardRepository
            .findById(id)
            .orElseThrow(() ->
                    new RuntimeException("Kart bulunamadı")
            );

    return toResponse(card);
    }
    public NfcCardResponse updateCard(
        Long id,
        UpdateNfcCardRequest request
    ) {

    NfcCard card = nfcCardRepository
            .findById(id)
            .orElseThrow(() ->
                    new RuntimeException("Kart bulunamadı")
            );

    card.setName(request.name());
    card.setType(request.type());
    card.setDestinationUrl(request.destinationUrl());

    NfcCard updatedCard = nfcCardRepository.save(card);

    return toResponse(updatedCard);
    }
}