package de.freeworldapp.app;

import de.freeworldapp.app.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Register → verify → login flow (AP 0.4). */
class AuthFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void registrationCreatesUnverifiedUserWithoutExposingEmail() throws Exception {
        mvc.perform(post("/api/users")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"reg_public","email":"reg_public@example.com","password":"password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("reg_public"))
                .andExpect(jsonPath("$.email").doesNotExist());

        User u = userRepository.findByUsername("reg_public").orElseThrow();
        assertThat(u.isEmailVerified()).isFalse();
        assertThat(u.getVerificationToken()).isNotBlank();
        assertThat(u.getVerificationTokenExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void loginBeforeEmailVerificationIsRejected() throws Exception {
        register("unverified_login");
        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"unverified_login","password":"password123"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void verifyThenLoginIssuesSessionToken() throws Exception {
        register("happy_path");
        verifyEmail("happy_path");

        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"happy_path","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.email").value("happy_path@example.com"));

        User u = userRepository.findByUsername("happy_path").orElseThrow();
        assertThat(u.isEmailVerified()).isTrue();
        assertThat(u.getVerificationToken()).isNull();
    }

    @Test
    void verifyWithUnknownTokenIs404() throws Exception {
        mvc.perform(get("/api/auth/verify").param("token", "no-such-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void verifyWithExpiredTokenIs410() throws Exception {
        register("expired_verify");
        User u = userRepository.findByUsername("expired_verify").orElseThrow();
        u.setVerificationTokenExpiresAt(Instant.now().minusSeconds(60));
        userRepository.save(u);

        mvc.perform(get("/api/auth/verify").param("token", u.getVerificationToken()))
                .andExpect(status().isGone());
    }

    @Test
    void loginWithWrongPasswordIs401() throws Exception {
        signUp("wrong_pw");
        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"wrong_pw","password":"not-the-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownUsernameIs401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ghost_user","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blockedUserCannotLogIn() throws Exception {
        signUp("blocked_login");
        User u = userRepository.findByUsername("blocked_login").orElseThrow();
        u.setBlocked(true);
        u.setBlockedAt(Instant.now());
        userRepository.save(u);

        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"blocked_login","password":"password123"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void resendVerificationNeverRevealsWhetherEmailExists() throws Exception {
        register("resend_user");
        // registered + unverified
        mvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"resend_user@example.com"}
                                """))
                .andExpect(status().isOk());
        // completely unknown address — same answer
        mvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void logoutInvalidatesTheSessionToken() throws Exception {
        AuthedUser user = signUp("logout_user");

        // token works before logout (sensitive GET)
        mvc.perform(get("/api/messages/unread-count")
                        .param("userId", user.id())
                        .header("X-Session-Token", user.token()))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/logout").header("X-Session-Token", user.token()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/messages/unread-count")
                        .param("userId", user.id())
                        .header("X-Session-Token", user.token()))
                .andExpect(status().isUnauthorized());
    }
}
