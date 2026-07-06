package de.freeworldapp.app;

import com.fasterxml.jackson.databind.JsonNode;
import de.freeworldapp.app.auth.RetentionCleanupJob;
import de.freeworldapp.app.auth.SessionRepository;
import de.freeworldapp.app.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AP 4.4: data export, password-confirmed anonymizing deletion, retention job. */
class DsgvoIntegrationTest extends IntegrationTestBase {

    @Autowired SessionRepository sessionRepository;
    @Autowired RetentionCleanupJob cleanupJob;

    @Test
    void exportContainsProfilePostsAndMessages() throws Exception {
        AuthedUser user = signUp("dsgvo_export");
        AuthedUser friend = signUp("dsgvo_friend");

        mvc.perform(post("/api/offers")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"export ladder","description":"d","region":"Leipzig",
                                 "category":"Other","quantity":1}
                                """))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/messages")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"senderId":"%s","recipientId":"%s","content":"export me"}
                                """.formatted(user.id(), friend.id())))
                .andExpect(status().is2xxSuccessful());

        MvcResult result = mvc.perform(get("/api/users/me/export")
                        .header("X-Session-Token", user.token()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("freeworld-export.json")))
                .andReturn();

        JsonNode export = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(export.get("profile").get("username").asText()).isEqualTo("dsgvo_export");
        assertThat(export.get("profile").get("email").asText()).isEqualTo("dsgvo_export@example.com");
        assertThat(export.get("offers").get(0).get("title").asText()).isEqualTo("export ladder");
        assertThat(export.get("messages").get(0).get("content").asText()).isEqualTo("export me");
        assertThat(export.get("messages").get(0).get("direction").asText()).isEqualTo("sent");

        mvc.perform(get("/api/users/me/export"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletionRequiresTheCorrectPassword() throws Exception {
        AuthedUser user = signUp("dsgvo_pw");

        mvc.perform(delete("/api/users/" + user.id())
                        .header("X-Session-Token", user.token()))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/users/" + user.id())
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"wrong-password"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletionAnonymizesButKeepsTheConversationForTheOtherSide() throws Exception {
        AuthedUser leaver = signUp("dsgvo_leaver");
        AuthedUser stayer = signUp("dsgvo_stayer");

        mvc.perform(post("/api/messages")
                        .header("X-Session-Token", leaver.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"senderId":"%s","recipientId":"%s","content":"bye bye"}
                                """.formatted(leaver.id(), stayer.id())))
                .andExpect(status().is2xxSuccessful());
        mvc.perform(post("/api/offers")
                        .header("X-Session-Token", leaver.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"leaver offer","description":"d","region":"Leipzig",
                                 "category":"Other","quantity":1}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/users/" + leaver.id())
                        .header("X-Session-Token", leaver.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"password123"}
                                """))
                .andExpect(status().isNoContent());

        // account scrubbed: no PII left, profile 404s, posts gone
        User scrubbed = userRepository.findById(java.util.UUID.fromString(leaver.id())).orElseThrow();
        assertThat(scrubbed.isDeleted()).isTrue();
        assertThat(scrubbed.getUsername()).startsWith("deleted-");
        assertThat(scrubbed.getEmail()).endsWith("@deleted.invalid");
        mvc.perform(get("/api/users/" + leaver.id())).andExpect(status().isNotFound());
        mvc.perform(get("/api/offers?offeredBy=" + leaver.id()))
                .andExpect(jsonPath("$.length()").value(0));

        // the stayer still sees the conversation, marked as deleted
        mvc.perform(get("/api/messages/conversations")
                        .param("userId", stayer.id())
                        .header("X-Session-Token", stayer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deleted").value(true))
                .andExpect(jsonPath("$[0].lastMessage").value("bye bye"));

        // nobody can message the anonymized account anymore
        mvc.perform(post("/api/messages")
                        .header("X-Session-Token", stayer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"senderId":"%s","recipientId":"%s","content":"hello?"}
                                """.formatted(stayer.id(), leaver.id())))
                .andExpect(status().isBadRequest());

        // and the leaver's session is gone
        mvc.perform(get("/api/messages/unread-count")
                        .param("userId", leaver.id())
                        .header("X-Session-Token", leaver.token()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void retentionJobPurgesExpiredSessionsAndTokens() throws Exception {
        AuthedUser user = signUp("dsgvo_retention");
        AuthedUser fresh = signUp("dsgvo_fresh");

        var session = sessionRepository.findByRawToken(user.token()).orElseThrow();
        session.setExpiresAt(Instant.now().minusSeconds(3600));
        sessionRepository.save(session);

        cleanupJob.run();

        assertThat(sessionRepository.findByRawToken(user.token())).isEmpty();
        assertThat(sessionRepository.findByRawToken(fresh.token())).isPresent();
    }
}
