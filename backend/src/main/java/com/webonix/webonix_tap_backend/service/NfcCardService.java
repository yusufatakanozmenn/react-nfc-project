package com.webonix.webonix_tap_backend.service;

import com.webonix.webonix_tap_backend.dto.*;
import com.webonix.webonix_tap_backend.entity.AppUser;
import com.webonix.webonix_tap_backend.entity.NfcCard;
import com.webonix.webonix_tap_backend.repository.AppUserRepository;
import com.webonix.webonix_tap_backend.repository.NfcCardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class NfcCardService {
    private final NfcCardRepository cards;
    private final AppUserRepository users;
    private final AuthService auth;

    public NfcCardService(NfcCardRepository cards, AppUserRepository users, AuthService auth) {
        this.cards = cards;
        this.users = users;
        this.auth = auth;
    }

    private boolean isAdmin(AppUser actor) { return "ADMIN".equals(actor.getRole()); }
    private AppUser actor(String userId) {
        AppUser actor = auth.requireActiveUser(userId);
        if (!isAdmin(actor) && !"USER".equals(actor.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return actor;
    }
    private void requireAdmin(String userId) {
        if (!isAdmin(actor(userId))) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    private NfcCard accessibleCard(Long id, String userId) {
        AppUser actor = actor(userId);
        // Query ownership in the database; other users' IDs look exactly like missing cards.
        return (isAdmin(actor) ? cards.findById(id) : cards.findByIdAndOwner_Id(id, actor.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kart bulunamadı."));
    }
    private List<NfcCard> accessibleCards(String userId) {
        AppUser actor = actor(userId);
        return isAdmin(actor) ? cards.findAllByOrderByIdDesc() : cards.findByOwner_IdOrderByIdDesc(actor.getId());
    }
    private AppUser owner(Long id) {
        if (id == null) return null;
        return users.findById(id).filter(user -> Boolean.TRUE.equals(user.getActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktif bir kullanıcı seçin."));
    }

    @Transactional(readOnly = true)
    public List<NfcCardResponse> getAllCards(String userId) {
        return accessibleCards(userId).stream().map(this::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public NfcCardResponse getCardById(Long id, String userId) { return toResponse(accessibleCard(id, userId)); }

    @Transactional(readOnly = true)
    public StatisticsResponse statistics(String userId) {
        List<NfcCardResponse> visible = getAllCards(userId);
        long active = visible.stream().filter(card -> Boolean.TRUE.equals(card.active())).count();
        long scans = visible.stream().mapToLong(card -> card.scans() == null ? 0L : card.scans().longValue()).sum();
        return new StatisticsResponse(visible.size(), active, visible.size() - active, scans, visible);
    }

    public NfcCardResponse createCard(CreateNfcCardRequest request, String userId) {
        requireAdmin(userId);
        validate(request.name(), request.type(), request.destinationUrl());
        NfcCard card = new NfcCard(request.name().trim(), request.type(), generateUniqueCode(),
                request.destinationUrl().trim(), 0, true);
        card.setOwner(owner(request.ownerId()));
        return toResponse(cards.save(card));
    }
    public NfcCardResponse updateCard(Long id, UpdateNfcCardRequest request, String userId) {
        NfcCard card = accessibleCard(id, userId);
        validate(request.name(), request.type(), request.destinationUrl());
        card.setName(request.name().trim());
        card.setType(request.type());
        card.setDestinationUrl(request.destinationUrl().trim());
        return toResponse(cards.save(card));
    }
    public NfcCardResponse toggleStatus(Long id, String userId) {
        NfcCard card = accessibleCard(id, userId);
        card.setActive(!Boolean.TRUE.equals(card.getActive()));
        return toResponse(cards.save(card));
    }
    public void deleteCard(Long id, String userId) {
        requireAdmin(userId);
        cards.delete(accessibleCard(id, userId));
    }
    public NfcCardResponse assignOwner(Long id, Long ownerId, String userId) {
        requireAdmin(userId);
        NfcCard card = accessibleCard(id, userId);
        card.setOwner(owner(ownerId));
        return toResponse(cards.save(card));
    }
    private String generateUniqueCode() {
        String code;
        do { code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(); }
        while (cards.existsByCode(code));
        return code;
    }
    private void validate(String name, String type, String url) {
        if (name == null || name.isBlank() || name.trim().length() > 255
                || type == null || !Set.of("google", "instagram", "whatsapp", "website").contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçerli kart adı ve türü girin.");
        }
        try {
            URI uri = URI.create(url == null ? "" : url.trim());
            if (url == null || url.trim().length() > 255 || uri.getHost() == null || uri.getUserInfo() != null
                    || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçerli bir http/https adresi girin (en fazla 255 karakter).");
        }
    }
    private NfcCardResponse toResponse(NfcCard card) {
        AppUser owner = card.getOwner();
        return new NfcCardResponse(card.getId(), card.getName(), card.getType(), card.getCode(),
                card.getDestinationUrl(), card.getScans(), card.getActive(),
                owner == null ? null : owner.getId(), owner == null ? null : owner.getName());
    }
}
