package de.freeworldapp.app;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AP 1.5: Bucket4j per-IP limits and account-based login lockout. */
class RateLimitIntegrationTest extends IntegrationTestBase {

    private void postForgot(String ip, int expectedStatus) throws Exception {
        mvc.perform(post("/api/auth/forgot-password")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"whoever@example.com"}
                                """))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void forgotPasswordAllowsFivePerQuarterHourThenRejects() throws Exception {
        for (int i = 0; i < 5; i++) postForgot(clientIp, 200);
        postForgot(clientIp, 429);
    }

    @Test
    void limitsAreTrackedPerIp() throws Exception {
        for (int i = 0; i < 5; i++) postForgot(clientIp, 200);
        postForgot(clientIp, 429);
        // a different source IP is unaffected
        postForgot("10.99.99.99", 200);
    }

    @Test
    void resendVerificationAllowsThreePerQuarterHour() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/resend-verification")
                            .header("X-Forwarded-For", clientIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"whoever@example.com"}
                                    """))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/auth/resend-verification")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"whoever@example.com"}
                                """))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void imageUploadsAreLimitedToTenPerMinute() throws Exception {
        // the limiter sits before auth, so unauthenticated 401s still consume tokens
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/images").header("X-Forwarded-For", clientIp))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/images").header("X-Forwarded-For", clientIp))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void reportsAreLimitedToFivePerMinute() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/reports").header("X-Forwarded-For", clientIp))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/reports").header("X-Forwarded-For", clientIp))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void tenFailedLoginsLockTheAccountEvenWithCorrectPassword() throws Exception {
        signUp("lockout_victim");

        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", clientIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"lockout_victim","password":"wrong-password"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        // even the correct password is rejected while locked
        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"lockout_victim","password":"password123"}
                                """))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void failedAttemptsResetOnSuccessfulLogin() throws Exception {
        signUp("lockout_reset");

        for (int i = 0; i < 9; i++) {
            mvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", clientIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"lockout_reset","password":"wrong-password"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        // successful login clears the counter…
        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"lockout_reset","password":"password123"}
                                """))
                .andExpect(status().isOk());

        // …so one more failure does NOT lock
        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"lockout_reset","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"lockout_reset","password":"password123"}
                                """))
                .andExpect(status().isOk());
    }
}
