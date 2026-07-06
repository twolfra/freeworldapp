package de.freeworldapp.app;

import de.freeworldapp.app.auth.PasswordResetTokenRepository;
import de.freeworldapp.app.email.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AP 1.1: forgot-password / reset-password flow.
 * EmailService is mocked so the raw reset token (only ever sent by mail —
 * the DB stores just its hash) can be captured.
 */
class PasswordResetIntegrationTest extends IntegrationTestBase {

    @MockitoBean
    EmailService emailService;

    @Autowired
    PasswordResetTokenRepository resetRepository;

    private void requestReset(String email) throws Exception {
        mvc.perform(post("/api/auth/forgot-password")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk());
    }

    private String captureResetToken(String email) {
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(org.mockito.ArgumentMatchers.eq(email),
                anyString(), token.capture(), anyString());
        return token.getValue();
    }

    @Test
    void happyPathResetsPasswordAndInvalidatesSessions() throws Exception {
        AuthedUser user = signUp("reset_happy");
        requestReset("reset_happy@example.com");
        String token = captureResetToken("reset_happy@example.com");

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"brandnewpassword1"}
                                """.formatted(token)))
                .andExpect(status().isOk());

        // all sessions are gone
        mvc.perform(get("/api/messages/unread-count")
                        .param("userId", user.id())
                        .header("X-Session-Token", user.token()))
                .andExpect(status().isUnauthorized());

        // old password rejected, new password works
        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"reset_happy","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"reset_happy","password":"brandnewpassword1"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void tokenCannotBeUsedTwice() throws Exception {
        signUp("reset_twice");
        requestReset("reset_twice@example.com");
        String token = captureResetToken("reset_twice@example.com");

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"brandnewpassword1"}
                                """.formatted(token)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"anotherpassword12"}
                                """.formatted(token)))
                .andExpect(status().isGone());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        signUp("reset_expired");
        requestReset("reset_expired@example.com");
        String token = captureResetToken("reset_expired@example.com");

        var t = resetRepository.findByRawToken(token).orElseThrow();
        t.setExpiresAt(Instant.now().minusSeconds(60));
        resetRepository.save(t);

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"brandnewpassword1"}
                                """.formatted(token)))
                .andExpect(status().isGone());
    }

    @Test
    void unknownTokenIs404() throws Exception {
        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"definitely-not-a-token","newPassword":"brandnewpassword1"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void weakPasswordIsRejectedByPolicy() throws Exception {
        signUp("reset_weak");
        requestReset("reset_weak@example.com");
        String token = captureResetToken("reset_weak@example.com");

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"short"}
                                """.formatted(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPasswordNeverRevealsWhetherEmailExists() throws Exception {
        long tokensBefore = resetRepository.count();
        requestReset("nobody-registered@example.com"); // 200 asserted inside
        verify(emailService, never())
                .sendPasswordResetEmail(any(), any(), any(), any());
        assertThat(resetRepository.count()).isEqualTo(tokensBefore);
    }

    @Test
    void tokenIsStoredOnlyHashed() throws Exception {
        signUp("reset_hashed");
        requestReset("reset_hashed@example.com");
        String token = captureResetToken("reset_hashed@example.com");

        assertThat(token).hasSize(43);
        assertThat(resetRepository.findAll())
                .isNotEmpty()
                .noneMatch(t -> t.getTokenHash().equals(token));
        assertThat(resetRepository.findByRawToken(token)).isPresent();
    }
}
