package com.webonix.webonix_tap_backend;

import com.webonix.webonix_tap_backend.entity.*;
import com.webonix.webonix_tap_backend.repository.*;
import com.webonix.webonix_tap_backend.service.SessionService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Real Hibernate/SQL transactions in disposable in-memory H2; never connects to MySQL. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:security;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PersistenceSecurityTests {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> UUID.randomUUID().toString().repeat(2));
    }
    @Autowired EntityManagerFactory factory;
    @Autowired AppUserRepository users;
    @Autowired NfcCardRepository cards;
    @Autowired SessionService sessions;
    @Autowired AuthSessionRepository sessionRepository;
    AppUser user(String name) { return users.saveAndFlush(new AppUser(name, UUID.randomUUID()+"@example.test", "unused-test-hash", "USER", true)); }

    @Test void oldOwnersConcurrentEditCannotOverwriteAnOwnershipTransfer() {
        AppUser oldOwner = user("old"), newOwner = user("new");
        var card = new NfcCard("Before", "website", UUID.randomUUID().toString(), "https://example.test", 0, true);
        card.setOwner(oldOwner); Long id = cards.saveAndFlush(card).getId();
        var first = factory.createEntityManager(); var second = factory.createEntityManager();
        try {
            first.getTransaction().begin();
            var stale = first.find(NfcCard.class, id);
            assertEquals(oldOwner.getId(), stale.getOwner().getId());
            second.getTransaction().begin();
            second.find(NfcCard.class, id).setOwner(second.getReference(AppUser.class, newOwner.getId()));
            second.getTransaction().commit();
            stale.setName("Unauthorized stale edit");
            assertThrows(RollbackException.class, () -> first.getTransaction().commit());
            var stored = cards.findById(id).orElseThrow();
            assertEquals(newOwner.getId(), stored.getOwner().getId());
            assertEquals("Before", stored.getName());
        } finally {
            if (first.getTransaction().isActive()) first.getTransaction().rollback();
            if (second.getTransaction().isActive()) second.getTransaction().rollback();
            first.close(); second.close();
        }
    }
    @Test void issuedSessionsPersistOnlyHashesAndRevocationSurvivesFreshReads() {
        AppUser user = user("session"); String token = sessions.issue(user.getId(), "USER");
        assertTrue(sessions.isActive(token, user.getId()));
        assertFalse(sessions.isActive(token, user.getId() + 100));
        assertFalse(sessionRepository.findAll().stream().anyMatch(s -> s.getId().equals(token)));
        sessions.revoke(token);
        assertFalse(sessions.isActive(token, user.getId()));
    }
}
