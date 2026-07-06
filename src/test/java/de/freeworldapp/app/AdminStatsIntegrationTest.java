package de.freeworldapp.app;

import com.fasterxml.jackson.databind.JsonNode;
import de.freeworldapp.app.user.Role;
import de.freeworldapp.app.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AP 4.3: admin statistics — aggregates only, nothing person-related. */
class AdminStatsIntegrationTest extends IntegrationTestBase {

    private AuthedUser signUpAdmin(String username) throws Exception {
        register(username);
        verifyEmail(username);
        User u = userRepository.findByUsername(username).orElseThrow();
        u.setRole(Role.ADMIN);
        userRepository.save(u);
        return login(username);
    }

    @Test
    void statsAggregateCountsAndWeeklySeries() throws Exception {
        AuthedUser admin = signUpAdmin("stats_admin");
        AuthedUser user = signUp("stats_user");
        MvcResult created = mvc.perform(post("/api/offers")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"stats offer","description":"d","region":"L",
                                 "category":"Other","quantity":1}
                                """))
                .andExpect(status().isCreated()).andReturn();
        String offerId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post("/api/offers/" + offerId + "/status")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GIVEN\"}"))
                .andExpect(status().isOk());

        MvcResult result = mvc.perform(get("/api/admin/stats")
                        .header("X-Session-Token", admin.token()))
                .andExpect(status().isOk()).andReturn();
        JsonNode stats = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(stats.get("totalUsers").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(stats.get("completedGifts").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(stats.get("registrationsPerWeek")).hasSize(8);
        assertThat(stats.get("registrationsPerWeek").get(7).get("count").asLong())
                .as("current week contains the fresh registrations")
                .isGreaterThanOrEqualTo(2);
        assertThat(stats.get("postsPerWeek")).hasSize(8);
        assertThat(stats.get("messagesPerWeek")).hasSize(8);
    }

    @Test
    void statsAreAdminOnly() throws Exception {
        AuthedUser user = signUp("stats_pleb");
        mvc.perform(get("/api/admin/stats").header("X-Session-Token", user.token()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
    }
}
