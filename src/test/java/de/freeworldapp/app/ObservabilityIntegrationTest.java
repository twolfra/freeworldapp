package de.freeworldapp.app;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AP 4.5: health probe + request-id correlation. */
class ObservabilityIntegrationTest extends IntegrationTestBase {

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        // not registered as endpoints — the SPA catch-all answers with HTML,
        // so assert no actuator JSON ever leaks
        MvcResult env = mvc.perform(get("/actuator/env")).andReturn();
        assertThat(env.getResponse().getContentAsString())
                .doesNotContain("activeProfiles").doesNotContain("propertySources");
        MvcResult beans = mvc.perform(get("/actuator/beans")).andReturn();
        assertThat(beans.getResponse().getContentAsString()).doesNotContain("\"beans\"");
    }

    @Test
    void everyResponseCarriesARequestId() throws Exception {
        MvcResult first = mvc.perform(get("/api/offers"))
                .andExpect(header().exists("X-Request-Id"))
                .andReturn();
        MvcResult second = mvc.perform(get("/api/offers")).andReturn();

        String id1 = first.getResponse().getHeader("X-Request-Id");
        String id2 = second.getResponse().getHeader("X-Request-Id");
        assertThat(id1).isNotBlank().isNotEqualTo(id2);

        // errors carry it too — that's the whole point of the correlation
        mvc.perform(get("/api/messages/unread-count").param("userId", "x"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void incomingRequestIdIsHonoured() throws Exception {
        mvc.perform(get("/api/offers").header("X-Request-Id", "lb-abc-12345"))
                .andExpect(header().string("X-Request-Id", "lb-abc-12345"));
    }
}
