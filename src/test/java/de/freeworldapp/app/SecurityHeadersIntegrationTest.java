package de.freeworldapp.app;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AP 1.3: security headers on every response. */
class SecurityHeadersIntegrationTest extends IntegrationTestBase {

    @Test
    void everyResponseCarriesTheSecurityHeaders() throws Exception {
        mvc.perform(get("/api/offers"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("img-src 'self' https://storage.googleapis.com data: blob:")))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("connect-src 'self' wss:")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", "camera=(), geolocation=(self), microphone=()"));
    }

    @Test
    void headersAreAlsoSetOnErrorResponses() throws Exception {
        mvc.perform(get("/api/messages/unread-count").param("userId", "x"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void hstsOnlyOverHttps() throws Exception {
        mvc.perform(get("/api/offers"))
                .andExpect(header().doesNotExist("Strict-Transport-Security"));

        mvc.perform(get("/api/offers").header("X-Forwarded-Proto", "https"))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000; includeSubDomains"));

        mvc.perform(get("/api/offers").secure(true))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000; includeSubDomains"));
    }

    @Test
    void enforcingModeDoesNotSetReportOnlyHeader() throws Exception {
        mvc.perform(get("/api/offers"))
                .andExpect(header().doesNotExist("Content-Security-Policy-Report-Only"));
    }
}
