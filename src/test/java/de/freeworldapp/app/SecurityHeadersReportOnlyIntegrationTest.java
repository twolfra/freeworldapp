package de.freeworldapp.app;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/** AP 1.3: CSP_REPORT_ONLY=true switches CSP to report-only for safe trialling. */
@TestPropertySource(properties = "app.security.csp-report-only=true")
class SecurityHeadersReportOnlyIntegrationTest extends IntegrationTestBase {

    @Test
    void reportOnlyModeUsesTheReportOnlyHeader() throws Exception {
        mvc.perform(get("/api/offers"))
                .andExpect(header().string("Content-Security-Policy-Report-Only",
                        org.hamcrest.Matchers.containsString("default-src 'self'")))
                .andExpect(header().doesNotExist("Content-Security-Policy"));
    }
}
