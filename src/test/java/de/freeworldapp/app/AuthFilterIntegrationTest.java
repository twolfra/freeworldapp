package de.freeworldapp.app;

import de.freeworldapp.app.auth.Session;
import de.freeworldapp.app.auth.SessionRepository;
import de.freeworldapp.app.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AuthFilter: public vs. protected paths, expired sessions, blocked accounts (AP 0.4). */
class AuthFilterIntegrationTest extends IntegrationTestBase {

    @Autowired
    SessionRepository sessionRepository;

    private static final String OFFER_BODY = """
            {"title":"Books","description":"A box of novels","region":"Leipzig",
             "category":"Books & Media","quantity":1}
            """;

    @Test
    void publicGetsWorkWithoutToken() throws Exception {
        mvc.perform(get("/api/offers")).andExpect(status().isOk());
        mvc.perform(get("/api/requests")).andExpect(status().isOk());
        mvc.perform(get("/api/users")).andExpect(status().isOk());
    }

    @Test
    void sensitiveGetsRequireToken() throws Exception {
        AuthedUser user = signUp("filter_sensitive");
        mvc.perform(get("/api/messages/conversations").param("userId", user.id()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/messages/unread-count").param("userId", user.id()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/subscriptions/feed").param("subscriberId", user.id()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mutatingRequestWithoutTokenIsRejected() throws Exception {
        mvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OFFER_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mutatingRequestWithInvalidTokenIsRejected() throws Exception {
        mvc.perform(post("/api/offers")
                        .header("X-Session-Token", "not-a-real-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OFFER_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenAllowsMutation() throws Exception {
        AuthedUser user = signUp("filter_valid");
        mvc.perform(post("/api/offers")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OFFER_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void sensitiveGetForAnotherUsersDataIs403() throws Exception {
        AuthedUser alice = signUp("filter_alice");
        AuthedUser bob = signUp("filter_bob");

        mvc.perform(get("/api/messages/conversations")
                        .param("userId", bob.id())
                        .header("X-Session-Token", alice.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void expiredSessionIsRejectedAndDeleted() throws Exception {
        AuthedUser user = signUp("filter_expired");
        Session session = sessionRepository.findByToken(user.token()).orElseThrow();
        session.setExpiresAt(Instant.now().minusSeconds(60));
        sessionRepository.save(session);

        mvc.perform(post("/api/offers")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OFFER_BODY))
                .andExpect(status().isUnauthorized());

        assertThat(sessionRepository.findByToken(user.token())).isEmpty();
    }

    @Test
    void blockedAccountsLiveSessionIsRejected() throws Exception {
        AuthedUser user = signUp("filter_blocked");
        User u = userRepository.findByUsername("filter_blocked").orElseThrow();
        u.setBlocked(true);
        u.setBlockedAt(Instant.now());
        userRepository.save(u);

        mvc.perform(post("/api/offers")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OFFER_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicAuthEndpointsNeedNoToken() throws Exception {
        // wrong creds → 401 from the controller, not "Authentication required" from the filter
        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"nobody_here","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"whoever@example.com"}
                                """))
                .andExpect(status().isOk());
    }
}
