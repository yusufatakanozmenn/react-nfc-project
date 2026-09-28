package com.webonix.webonix_tap_backend;

import com.webonix.webonix_tap_backend.entity.*;
import com.webonix.webonix_tap_backend.repository.*;
import com.webonix.webonix_tap_backend.service.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:recovery;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "app.frontend-url=http://localhost:5173", "app.cookies.secure=false"})
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class PasswordRecoveryTests {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> UUID.randomUUID().toString().repeat(2));
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired PasswordResetService recovery;
    @Autowired SessionService sessions;
    @Autowired JwtService jwt;
    @Autowired AppUserRepository users;
    @Autowired PasswordResetRepository resets;
    @Autowired PasswordEncoder encoder;
    @MockitoBean PasswordResetMailer mailer;
    AppUser user;
    String oldPassword, newPassword;
    record Delivery(String email, String link) {}
    final BlockingQueue<Delivery> outbox = new LinkedBlockingQueue<>();
    final AtomicInteger addresses = new AtomicInteger();
    @BeforeEach void setup() {
        oldPassword=UUID.randomUUID().toString(); newPassword=UUID.randomUUID().toString();
        user=users.saveAndFlush(new AppUser("Recovery",UUID.randomUUID()+"@example.test",encoder.encode(oldPassword),"USER",true));
        when(mailer.available()).thenReturn(true);
        doAnswer(call -> { outbox.add(new Delivery(call.getArgument(0),call.getArgument(1))); return null; }).when(mailer).sendLink(anyString(),anyString());
    }
    RequestPostProcessor csrf() throws Exception {
        var response=mvc.perform(get("/api/auth/csrf")).andReturn().getResponse();
        String token=mapper.readTree(response.getContentAsString()).get("token").asString();
        Cookie cookie=response.getCookie("webonix_csrf");
        return request -> { var cookies=new ArrayList<Cookie>(); if(request.getCookies()!=null) cookies.addAll(List.of(request.getCookies())); cookies.add(cookie);
            request.setCookies(cookies.toArray(Cookie[]::new)); request.addHeader("X-XSRF-TOKEN",token); request.setRemoteAddr("192.0.2."+addresses.incrementAndGet()); return request; };
    }
    ResultActions postJson(String path,Object body) throws Exception {
        return mvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }
    String linkToken() throws Exception {
        Delivery sent=outbox.poll(8,TimeUnit.SECONDS); assertNotNull(sent,"Expected test mail delivery");
        assertEquals(user.getEmail(),sent.email()); assertTrue(sent.link().startsWith("http://localhost:5173/reset-password#token="));
        return sent.link().split("#token=",2)[1];
    }
    String issue() throws Exception { recovery.request(user.getEmail()); return linkToken(); }
    @Test void rememberMeControlsCookiePersistenceAndJwtLifetime() throws Exception {
        for(boolean remember:new boolean[]{false,true}) {
            var response=postJson("/api/auth/login",Map.of("email",user.getEmail(),"password",oldPassword,"rememberMe",remember))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").doesNotExist()).andReturn().getResponse();
            Cookie cookie=response.getCookie("webonix_session"); assertTrue(cookie.isHttpOnly());
            assertEquals(remember?604800:-1,cookie.getMaxAge());
            var claims=jwt.getClaims(cookie.getValue()); assertEquals(remember?604800000L:3600000L,claims.getExpiration().getTime()-claims.getIssuedAt().getTime());
            assertTrue(sessions.isActive(cookie.getValue(),user.getId()));
        }
    }
    @Test void omittedRememberMeDefaultsToSessionCookie() throws Exception {
        postJson("/api/auth/login",Map.of("email",user.getEmail(),"password",oldPassword))
            .andExpect(status().isOk()).andExpect(cookie().maxAge("webonix_session",-1));
    }
    @Test void requestReturnsGenericResponseAndTokenOnlyThroughMail() throws Exception {
        var known=postJson("/api/auth/forgot-password",Map.of("email",user.getEmail().toUpperCase()))
            .andExpect(status().isAccepted()).andExpect(jsonPath("$.token").doesNotExist()).andReturn().getResponse();
        String token=linkToken(); var stored=resets.findById(user.getId()).orElseThrow();
        assertFalse(stored.getTokenHash().equals(token)); assertEquals(64,stored.getTokenHash().length());
        assertTrue(stored.getExpiresAt().isBefore(Instant.now().plusSeconds(901)));
        var unknown=postJson("/api/auth/forgot-password",Map.of("email",UUID.randomUUID()+"@example.test"))
            .andExpect(status().isAccepted()).andReturn().getResponse();
        assertEquals(known.getContentAsString(),unknown.getContentAsString());
    }
    @Test void resetChangesPasswordRevokesAllSessionsAndCannotBeReused() throws Exception {
        String normal=sessions.issue(user.getId(),"USER",false), remembered=sessions.issue(user.getId(),"USER",true);
        String token=issue();
        postJson("/api/auth/reset-password",Map.of("token",token,"password",newPassword)).andExpect(status().isOk());
        var updated=users.findById(user.getId()).orElseThrow();
        assertTrue(encoder.matches(newPassword,updated.getPassword())); assertFalse(encoder.matches(oldPassword,updated.getPassword()));
        assertFalse(sessions.isActive(normal,user.getId())); assertFalse(sessions.isActive(remembered,user.getId()));
        assertTrue(resets.findById(user.getId()).isEmpty());
        postJson("/api/auth/reset-password",Map.of("token",token,"password",newPassword)).andExpect(status().isBadRequest());
        postJson("/api/auth/login",Map.of("email",user.getEmail(),"password",oldPassword)).andExpect(status().isUnauthorized());
        postJson("/api/auth/login",Map.of("email",user.getEmail(),"password",newPassword)).andExpect(status().isOk());
    }
    @Test void newRequestInvalidatesPreviousLink() throws Exception {
        String first=issue(),second=issue();
        assertThrows(ResponseStatusException.class,()->recovery.reset(first,newPassword));
        assertDoesNotThrow(()->recovery.reset(second,newPassword));
    }
    @Test void expiredLinksAreRejectedWithoutChangingPassword() throws Exception {
        String token=issue(); var record=resets.findById(user.getId()).orElseThrow();
        resets.saveAndFlush(new PasswordReset(user.getId(),record.getTokenHash(),user.getEmail(),Instant.now().minusSeconds(1)));
        assertThrows(ResponseStatusException.class,()->recovery.reset(token,newPassword));
        assertTrue(encoder.matches(oldPassword,users.findById(user.getId()).orElseThrow().getPassword()));
    }
    @Test void emailChangeOrDisabledAccountInvalidatesLink() throws Exception {
        String token=issue(); user.setEmail(UUID.randomUUID()+"@example.test"); users.saveAndFlush(user);
        assertThrows(ResponseStatusException.class,()->recovery.reset(token,newPassword));
        String next=issue(); user.setActive(false); users.saveAndFlush(user);
        assertThrows(ResponseStatusException.class,()->recovery.reset(next,newPassword));
    }
    @Test void badPasswordsDoNotConsumeValidLink() throws Exception {
        String token=issue();
        for(String password:List.of("short","x".repeat(73),"ş".repeat(37)))
            assertThrows(ResponseStatusException.class,()->recovery.reset(token,password));
        assertDoesNotThrow(()->recovery.reset(token,UUID.randomUUID().toString().substring(0,6)));
    }
    @Test void forgedOrMissingTokensAreRejected() throws Exception {
        postJson("/api/auth/reset-password",Map.of("token","forged","password",newPassword)).andExpect(status().isBadRequest());
        postJson("/api/auth/reset-password",Map.of("token","a".repeat(43),"password",newPassword)).andExpect(status().isBadRequest());
    }
    @Test void recoveryEndpointsRequireCsrf() throws Exception {
        for(String path:List.of("/api/auth/forgot-password","/api/auth/reset-password"))
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }
    @Test void mailUnavailableDoesNotClaimThatMailWasSent() throws Exception {
        when(mailer.available()).thenReturn(false);
        postJson("/api/auth/forgot-password",Map.of("email",user.getEmail())).andExpect(status().isServiceUnavailable());
        assertTrue(resets.findById(user.getId()).isEmpty()); verify(mailer,never()).sendLink(anyString(),anyString());
    }
    @Test void smtpFailureRollsBackTokenReplacement() throws Exception {
        String token=issue();
        doThrow(new IllegalStateException("Test transport failure")).when(mailer).sendLink(anyString(),anyString());
        assertThrows(IllegalStateException.class,()->recovery.request(user.getEmail()));
        assertDoesNotThrow(()->recovery.reset(token,newPassword));
    }
    @Test void disabledAndUnknownAccountsDoNotReceiveEmail() {
        user.setActive(false); users.saveAndFlush(user);
        recovery.request(user.getEmail()); recovery.request(UUID.randomUUID()+"@example.test");
        verify(mailer,never()).sendLink(anyString(),anyString());
    }
    @Test void requestsAreLimitedPerAccountAcrossAddresses() throws Exception {
        String missing=UUID.randomUUID()+"@example.test";
        for(int i=0;i<3;i++) postJson("/api/auth/forgot-password",Map.of("email",missing)).andExpect(status().isAccepted());
        postJson("/api/auth/forgot-password",Map.of("email",missing.toUpperCase())).andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
    }
    @Test void sameTokenCannotBeUsedByTwoConcurrentTransactions() throws Exception {
        String token=issue();
        try(var executor=Executors.newFixedThreadPool(2)) {
            Callable<Boolean> reset=()->{try { recovery.reset(token,UUID.randomUUID().toString()); return true; } catch(ResponseStatusException rejected) { return false; }};
            var first=executor.submit(reset); var second=executor.submit(reset);
            assertNotEquals(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS));
        }
    }
    ResultActions changePassword(String session, Object body) throws Exception {
        return mvc.perform(put("/api/auth/me/password").cookie(new Cookie("webonix_session",session))
            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }
    Map<String,String> passwordBody(String current, String next, String confirmation) {
        return Map.of("currentPassword",current,"newPassword",next,"confirmPassword",confirmation);
    }
    @Test void settingsPasswordChangeRevokesSessionsAndPendingRecoveryAndAcceptsSixCharacters() throws Exception {
        String normal=sessions.issue(user.getId(),"USER",false), remembered=sessions.issue(user.getId(),"USER",true);
        String resetToken=issue(), next=UUID.randomUUID().toString().substring(0,6);
        changePassword(normal,passwordBody(oldPassword,next,next)).andExpect(status().isNoContent())
            .andExpect(cookie().maxAge("webonix_session",0));
        assertTrue(encoder.matches(next,users.findById(user.getId()).orElseThrow().getPassword()));
        for(String token:List.of(normal,remembered)) {
            assertFalse(sessions.isActive(token,user.getId()));
            mvc.perform(get("/api/auth/me").cookie(new Cookie("webonix_session",token))).andExpect(status().isUnauthorized());
        }
        assertTrue(resets.findById(user.getId()).isEmpty());
        assertThrows(ResponseStatusException.class,()->recovery.reset(resetToken,newPassword));
        postJson("/api/auth/login",Map.of("email",user.getEmail(),"password",oldPassword)).andExpect(status().isUnauthorized());
        postJson("/api/auth/login",Map.of("email",user.getEmail(),"password",next)).andExpect(status().isOk());
    }
    @Test void settingsPasswordChangeCannotTargetAnotherAccountOrElevateRole() throws Exception {
        AppUser other=users.saveAndFlush(new AppUser("Other",UUID.randomUUID()+"@example.test",encoder.encode(newPassword),"ADMIN",true));
        String otherHash=other.getPassword(), ownSession=sessions.issue(user.getId(),"USER"), otherSession=sessions.issue(other.getId(),"ADMIN");
        issue();
        var body=new HashMap<String,Object>(passwordBody(oldPassword,newPassword,newPassword));
        body.put("id",other.getId()); body.put("userId",other.getId()); body.put("role","ADMIN"); body.put("active",false);
        changePassword(ownSession,body).andExpect(status().isNoContent());
        var updated=users.findById(user.getId()).orElseThrow();
        assertEquals("USER",updated.getRole()); assertTrue(updated.getActive());
        assertTrue(encoder.matches(newPassword,updated.getPassword()));
        assertEquals(otherHash,users.findById(other.getId()).orElseThrow().getPassword());
        assertTrue(sessions.isActive(otherSession,other.getId()));
    }
    @Test void adminCanAlsoChangeOwnPassword() throws Exception {
        user.setRole("ADMIN"); users.saveAndFlush(user);
        changePassword(sessions.issue(user.getId(),"ADMIN"),passwordBody(oldPassword,newPassword,newPassword)).andExpect(status().isNoContent());
        assertTrue(encoder.matches(newPassword,users.findById(user.getId()).orElseThrow().getPassword()));
    }
    @Test void invalidSettingsPasswordsPreservePasswordSessionsAndRecoveryLink() throws Exception {
        String session=sessions.issue(user.getId(),"USER"); issue();
        String hash=users.findById(user.getId()).orElseThrow().getPassword();
        for(var body:List.of(
            passwordBody("incorrect",newPassword,newPassword), passwordBody(oldPassword,newPassword,"mismatch"),
            passwordBody(oldPassword,"short","short"), passwordBody(oldPassword,"ş".repeat(37),"ş".repeat(37)),
            passwordBody(oldPassword,oldPassword,oldPassword), Map.<String,String>of())) {
            changePassword(session,body).andExpect(status().isBadRequest());
            assertEquals(hash,users.findById(user.getId()).orElseThrow().getPassword());
            assertTrue(sessions.isActive(session,user.getId())); assertTrue(resets.findById(user.getId()).isPresent());
        }
    }
    @Test void settingsPasswordChangeRequiresAuthenticatedActiveAccountAndCsrf() throws Exception {
        String session=sessions.issue(user.getId(),"USER");
        mvc.perform(put("/api/auth/me/password").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/auth/me/password").cookie(new Cookie("webonix_session",session)).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        user.setActive(false); users.saveAndFlush(user);
        changePassword(session,passwordBody(oldPassword,newPassword,newPassword)).andExpect(status().isUnauthorized());
    }
    @Test void settingsPasswordAttemptsAreLimitedAcrossAddresses() throws Exception {
        String session=sessions.issue(user.getId(),"USER");
        for(int i=0;i<10;i++) changePassword(session,passwordBody("incorrect",newPassword,newPassword)).andExpect(status().isBadRequest());
        changePassword(session,passwordBody(oldPassword,newPassword,newPassword)).andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"));
        assertTrue(encoder.matches(oldPassword,users.findById(user.getId()).orElseThrow().getPassword()));
    }

}
