package de.freeworldapp.app;

import de.freeworldapp.app.auth.SessionRepository;
import de.freeworldapp.app.auth.Tokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AP 1.2: session tokens are 256-bit and stored only as SHA-256 hashes; change-password. */
class SessionHardeningIntegrationTest extends IntegrationTestBase {

    @Autowired
    SessionRepository sessionRepository;

    @Test
    void sessionTokenIsLongRandomAndStoredOnlyHashed() throws Exception {
        AuthedUser user = signUp("hash_user");

        // 256-bit base64url token, not a 36-char UUID
        assertThat(user.token()).hasSize(43).doesNotContain("-");

        // DB contains the SHA-256 hash, never the raw token
        var session = sessionRepository.findByTokenHash(Tokens.sha256(user.token()));
        assertThat(session).isPresent();
        assertThat(session.get().getTokenHash()).isNotEqualTo(user.token());
        assertThat(sessionRepository.findAll())
                .noneMatch(s -> s.getTokenHash().equals(user.token()));
    }

    @Test
    void changePasswordRequiresCorrectOldPassword() throws Exception {
        AuthedUser user = signUp("chpw_wrong_old");
        mvc.perform(post("/api/auth/change-password")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"oldPassword":"not-the-password","newPassword":"longenoughpw123"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void changePasswordEnforcesMinimumLength() throws Exception {
        AuthedUser user = signUp("chpw_short");
        mvc.perform(post("/api/auth/change-password")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"oldPassword":"password123","newPassword":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordInvalidatesOtherSessionsButKeepsCurrent() throws Exception {
        signUp("chpw_sessions");
        AuthedUser first = login("chpw_sessions");
        AuthedUser second = login("chpw_sessions");

        mvc.perform(post("/api/auth/change-password")
                        .header("X-Session-Token", second.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"oldPassword":"password123","newPassword":"longenoughpw123"}
                                """))
                .andExpect(status().isOk());

        // the session used for the change survives…
        mvc.perform(get("/api/messages/unread-count")
                        .param("userId", second.id())
                        .header("X-Session-Token", second.token()))
                .andExpect(status().isOk());

        // …every other session is gone
        mvc.perform(get("/api/messages/unread-count")
                        .param("userId", first.id())
                        .header("X-Session-Token", first.token()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void newPasswordWorksAndOldOneDoesNot() throws Exception {
        AuthedUser user = signUp("chpw_relogin");
        mvc.perform(post("/api/auth/change-password")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"oldPassword":"password123","newPassword":"longenoughpw123"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"chpw_relogin","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"chpw_relogin","password":"longenoughpw123"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void changePasswordWithoutSessionIsRejected() throws Exception {
        mvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"oldPassword":"password123","newPassword":"longenoughpw123"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
