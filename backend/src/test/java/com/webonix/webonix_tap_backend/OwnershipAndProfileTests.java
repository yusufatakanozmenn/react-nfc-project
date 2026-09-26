package com.webonix.webonix_tap_backend;

import com.webonix.webonix_tap_backend.config.*;
import com.webonix.webonix_tap_backend.controller.*;
import com.webonix.webonix_tap_backend.entity.*;
import com.webonix.webonix_tap_backend.repository.*;
import com.webonix.webonix_tap_backend.security.JwtAuthenticationFilter;
import com.webonix.webonix_tap_backend.service.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, NfcCardController.class, StatisticsController.class, AdminUserController.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Import({SecurityConfig.class, WebConfig.class, JwtAuthenticationFilter.class, JwtService.class, AuthService.class, NfcCardService.class})
class OwnershipAndProfileTests {
    private static final String SECRET = UUID.randomUUID().toString().repeat(2);
    private static final String PASSWORD = UUID.randomUUID().toString();
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper mapper;
    @MockitoBean AppUserRepository users;
    @MockitoBean NfcCardRepository cards;
    AppUser user, other, admin;
    NfcCard ownCard, otherCard, unassigned;

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("app.jwt.secret", () -> SECRET); }
    AppUser account(long id, String role) {
        var account = new AppUser("User " + id, "user" + id + "@example.test", encoder.encode(PASSWORD), role, true);
        ReflectionTestUtils.setField(account, "id", id); return account;
    }
    NfcCard card(long id, AppUser owner, int scans, boolean active) {
        var card = new NfcCard("Card " + id, "website", "CODE" + id, "https://example.test", scans, active);
        ReflectionTestUtils.setField(card, "id", id); card.setOwner(owner); return card;
    }
    String token(long id) { return "Bearer " + jwt.generateToken(id, id == 3 ? "ADMIN" : "USER"); }
    String json(Object value) { return mapper.writeValueAsString(value); }
    Map<String, Object> cardBody() { return new HashMap<>(Map.of("name", "Updated", "type", "website", "destinationUrl", "https://changed.test")); }
    Map<String, Object> profile(String email) { return new HashMap<>(Map.of("name", "New Name", "email", email, "currentPassword", PASSWORD)); }
    ResultActions putJson(String path, long actor, Object body) throws Exception {
        return mvc.perform(put(path).header("Authorization", token(actor)).contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }
    ResultActions getAs(String path, long actor) throws Exception { return mvc.perform(get(path).header("Authorization", token(actor))); }

    @BeforeEach void setup() {
        user = account(1, "USER"); other = account(2, "USER"); admin = account(3, "ADMIN");
        var accounts = new ArrayList<>(List.of(user, other, admin));
        when(users.findByEmail(anyString())).thenAnswer(call -> accounts.stream().filter(u -> u.getEmail().equals(call.getArgument(0))).findFirst());
        when(users.findById(anyLong())).thenAnswer(call -> accounts.stream().filter(u -> u.getId().equals(call.getArgument(0))).findFirst());
        when(users.findAll()).thenReturn(accounts);
        when(users.findByRoleOrderByNameAsc(anyString())).thenAnswer(call -> accounts.stream()
                .filter(account -> account.getRole().equals(call.getArgument(0))).toList());
        when(users.existsByEmail(anyString())).thenAnswer(call -> accounts.stream()
                .anyMatch(account -> account.getEmail().equals(call.getArgument(0))));
        when(users.saveAndFlush(any())).thenAnswer(call -> {
            AppUser account = call.getArgument(0);
            if (account.getId() == null) {
                ReflectionTestUtils.setField(account, "id", 4L);
                accounts.add(account);
            }
            return account;
        });
        ownCard = card(101, user, 5, true); otherCard = card(102, other, 50, false); unassigned = card(103, null, 7, true);
        var all = List.of(ownCard, otherCard, unassigned);
        when(cards.findAllByOrderByIdDesc()).thenReturn(all);
        when(cards.findByOwner_IdOrderByIdDesc(anyLong())).thenAnswer(call -> all.stream()
                .filter(c -> c.getOwner() != null && c.getOwner().getId().equals(call.getArgument(0))).toList());
        when(cards.findById(anyLong())).thenAnswer(call -> all.stream().filter(c -> c.getId().equals(call.getArgument(0))).findFirst());
        when(cards.findByIdAndOwner_Id(anyLong(), anyLong())).thenAnswer(call -> all.stream()
                .filter(c -> c.getId().equals(call.getArgument(0)) && c.getOwner() != null && c.getOwner().getId().equals(call.getArgument(1))).findFirst());
        when(cards.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test void userSeesOnlyOwnedCardsAndStatistics() throws Exception {
        getAs("/api/cards", 1).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(101));
        getAs("/api/statistics", 1).andExpect(status().isOk()).andExpect(jsonPath("$.totalCards").value(1))
                .andExpect(jsonPath("$.totalScans").value(5)).andExpect(jsonPath("$.activeCards").value(1)).andExpect(jsonPath("$.cards.length()").value(1));
        getAs("/api/statistics", 2).andExpect(status().isOk()).andExpect(jsonPath("$.totalScans").value(50));
    }
    @Test void otherAndUnassignedAndMissingIdsCannotBeReadEditedOrToggled() throws Exception {
        for (long id : new long[]{102, 103, 999}) {
            getAs("/api/cards/" + id, 1).andExpect(status().isNotFound());
            putJson("/api/cards/" + id, 1, cardBody()).andExpect(status().isNotFound());
            mvc.perform(patch("/api/cards/" + id + "/status").header("Authorization", token(1))).andExpect(status().isNotFound());
        }
        verify(cards, never()).save(any());
    }
    @Test void ownerCanEditAndToggleButNotChangeOwnerOrCode() throws Exception {
        var body = cardBody(); body.put("ownerId", 2); body.put("code", "FORGED");
        putJson("/api/cards/101", 1, body).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.ownerId").value(1)).andExpect(jsonPath("$.code").value("CODE101"));
        mvc.perform(patch("/api/cards/101/status").header("Authorization", token(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }
    @Test void userCannotCreateDeleteAssignOrListAccounts() throws Exception {
        mvc.perform(post("/api/cards").header("Authorization", token(1)).contentType(MediaType.APPLICATION_JSON).content(json(cardBody())))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/cards/101").header("Authorization", token(1))).andExpect(status().isForbidden());
        putJson("/api/cards/101/owner", 1, Map.of("ownerId", 2)).andExpect(status().isForbidden());
        getAs("/api/admin/users", 1).andExpect(status().isForbidden());
    }
    @Test void adminSeesAllCardsStatisticsAndSafeAccounts() throws Exception {
        getAs("/api/cards", 3).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        getAs("/api/statistics", 3).andExpect(status().isOk()).andExpect(jsonPath("$.totalCards").value(3)).andExpect(jsonPath("$.totalScans").value(62));
        getAs("/api/admin/users", 3).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3)).andExpect(jsonPath("$[0].password").doesNotExist());
    }
    @Test void adminCanAssignTransferAndUnassignCards() throws Exception {
        putJson("/api/cards/103/owner", 3, Map.of("ownerId", 1)).andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(1));
        getAs("/api/cards/103", 1).andExpect(status().isOk());
        putJson("/api/cards/103/owner", 3, Map.of("ownerId", 2)).andExpect(status().isOk());
        getAs("/api/cards/103", 1).andExpect(status().isNotFound());
        getAs("/api/cards/103", 2).andExpect(status().isOk());
        putJson("/api/cards/103/owner", 3, Collections.singletonMap("ownerId", null)).andExpect(status().isOk());
        getAs("/api/cards/103", 2).andExpect(status().isNotFound());
    }
    @Test void adminCanCreateEditToggleAndDeleteAnyCard() throws Exception {
        var body = cardBody(); body.put("ownerId", 2);
        mvc.perform(post("/api/cards").header("Authorization", token(3)).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ownerId").value(2));
        putJson("/api/cards/102", 3, cardBody()).andExpect(status().isOk());
        mvc.perform(patch("/api/cards/102/status").header("Authorization", token(3))).andExpect(status().isOk());
        mvc.perform(delete("/api/cards/102").header("Authorization", token(3))).andExpect(status().isNoContent());
        verify(cards).delete(otherCard);
    }
    @Test void missingAndInactiveOwnersAreRejected() throws Exception {
        other.setActive(false);
        putJson("/api/cards/101/owner", 3, Map.of("ownerId", 2)).andExpect(status().isBadRequest());
        putJson("/api/cards/101/owner", 3, Map.of("ownerId", 999)).andExpect(status().isBadRequest());
        verify(cards, never()).save(any());
    }
    @Test void unsafeUrlIsRejected() throws Exception {
        var body = cardBody(); body.put("destinationUrl", "javascript:alert(1)");
        putJson("/api/cards/101", 1, body).andExpect(status().isBadRequest());
    }
    @Test void profileChangesOnlySelfAndStableTokenSurvivesEmailChange() throws Exception {
        var body = profile("new@example.test"); body.put("id", 2); body.put("role", "ADMIN"); body.put("active", false);
        String before = token(1);
        putJson("/api/auth/me", 1, body).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("new@example.test")).andExpect(jsonPath("$.role").value("USER"));
        mvc.perform(get("/api/auth/me").header("Authorization", before)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("New Name"));
        getAs("/api/admin/users", 1).andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertEquals("user2@example.test", other.getEmail());
        org.junit.jupiter.api.Assertions.assertTrue(user.getActive());
    }
    @Test void profileRequiresCorrectPasswordAndUniqueValidEmail() throws Exception {
        var body = profile("new@example.test"); body.put("currentPassword", "wrong");
        putJson("/api/auth/me", 1, body).andExpect(status().isBadRequest());
        putJson("/api/auth/me", 1, profile("USER2@example.test")).andExpect(status().isConflict());
        putJson("/api/auth/me", 1, profile("invalid")).andExpect(status().isBadRequest());
        verify(users, never()).saveAndFlush(any());
    }
    @Test void tokenRoleCannotOverrideCurrentDatabaseRole() throws Exception {
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + jwt.generateToken(1L, "ADMIN"))).andExpect(status().isForbidden());
    }
    @Test void legacyEmailSubjectIsRejected() throws Exception {
        String legacy = Jwts.builder().subject(user.getEmail()).expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + legacy)).andExpect(status().isUnauthorized());
    }

    Map<String, Object> customerBody() {
        return new HashMap<>(Map.of("name", "  Yeni Müşteri  ", "email", "  CUSTOMER@example.test  ", "password", PASSWORD));
    }
    ResultActions createCustomerAs(long actor, Object body) throws Exception {
        return mvc.perform(post("/api/admin/customers").header("Authorization", token(actor))
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    @Test void customerListContainsOnlyUsersAndNeverCredentials() throws Exception {
        getAs("/api/admin/customers", 3).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1)).andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[0].password").doesNotExist()).andExpect(jsonPath("$[0].token").doesNotExist());
    }

    @Test void customersRequireAdminForBothListAndCreation() throws Exception {
        getAs("/api/admin/customers", 1).andExpect(status().isForbidden());
        createCustomerAs(1, customerBody()).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/customers")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/customers").contentType(MediaType.APPLICATION_JSON).content(json(customerBody())))
                .andExpect(status().isUnauthorized());
        verify(users, never()).saveAndFlush(any());
    }

    @Test void adminCreatesUserWithHashedPasswordAndNoCredentialResponse() throws Exception {
        var body = customerBody(); body.put("role", "ADMIN"); body.put("active", false); body.put("id", 1);
        createCustomerAs(3, body).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("Yeni Müşteri")).andExpect(jsonPath("$.email").value("customer@example.test"))
                .andExpect(jsonPath("$.active").value(true)).andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
        var created = users.findById(4L).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("USER", created.getRole());
        org.junit.jupiter.api.Assertions.assertTrue(encoder.matches(PASSWORD, created.getPassword()));
        org.junit.jupiter.api.Assertions.assertNotEquals(PASSWORD, created.getPassword());
        getAs("/api/auth/me", 3).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test void newCustomerCanLoginAndSeeOnlyTheCardAssignedByAdmin() throws Exception {
        createCustomerAs(3, customerBody()).andExpect(status().isCreated());
        getAs("/api/admin/users", 3).andExpect(status().isOk()).andExpect(jsonPath("$[3].id").value(4));
        var loginBody = Map.of("email", "customer@example.test", "password", PASSWORD);
        String result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(loginBody)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String customerToken = "Bearer " + mapper.readTree(result).get("token").asString();
        mvc.perform(get("/api/cards").header("Authorization", customerToken)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        putJson("/api/cards/103/owner", 3, Map.of("ownerId", 4)).andExpect(status().isOk());
        mvc.perform(get("/api/cards").header("Authorization", customerToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(103));
        mvc.perform(get("/api/statistics").header("Authorization", customerToken)).andExpect(status().isOk()).andExpect(jsonPath("$.totalScans").value(7));
        mvc.perform(get("/api/admin/customers").header("Authorization", customerToken)).andExpect(status().isForbidden());
    }

    @Test void duplicateCustomerEmailIsConflictIncludingCaseAndSpaces() throws Exception {
        var body = customerBody(); body.put("email", " USER1@example.test ");
        createCustomerAs(3, body).andExpect(status().isConflict());
        verify(users, never()).saveAndFlush(any());
    }

    @Test void concurrentDuplicateCustomerEmailIsConflict() throws Exception {
        doThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate email")).when(users).saveAndFlush(any());
        createCustomerAs(3, customerBody()).andExpect(status().isConflict());
    }

    @Test void invalidCustomerInputIsRejectedBeforeSaving() throws Exception {
        createCustomerAs(3, Map.of()).andExpect(status().isBadRequest());
        for (var change : List.of(Map.of("name", "  "), Map.of("name", "a".repeat(101)),
                Map.of("email", "invalid"), Map.of("email", "a".repeat(150) + "@example.test"),
                Map.of("password", "short"), Map.of("password", " ".repeat(8)), Map.of("password", "a".repeat(73)),
                Map.of("password", "ş".repeat(37)))) {
            var body = customerBody(); body.putAll(change);
            createCustomerAs(3, body).andExpect(status().isBadRequest());
        }
        verify(users, never()).saveAndFlush(any());
    }
}
