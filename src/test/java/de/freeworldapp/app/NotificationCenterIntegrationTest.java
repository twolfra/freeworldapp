package de.freeworldapp.app;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AP 3.4: in-app notification centre. */
class NotificationCenterIntegrationTest extends IntegrationTestBase {

    private String createOffer(AuthedUser owner, String title) throws Exception {
        MvcResult result = mvc.perform(post("/api/offers")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"d","region":"Leipzig",
                                 "category":"Other","quantity":1}
                                """.formatted(title)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private JsonNode notifications(AuthedUser user) throws Exception {
        MvcResult r = mvc.perform(get("/api/notifications").header("X-Session-Token", user.token()))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString());
    }

    private List<String> types(JsonNode response) {
        List<String> out = new ArrayList<>();
        response.get("items").forEach(n -> out.add(n.get("type").asText()));
        return out;
    }

    @Test
    void interestNotifiesTheOwner() throws Exception {
        AuthedUser owner = signUp("nc_owner");
        AuthedUser buyer = signUp("nc_buyer");
        String offerId = createOffer(owner, "nc ladder");

        mvc.perform(post("/api/offers/" + offerId + "/interest")
                        .header("X-Session-Token", buyer.token()))
                .andExpect(status().isOk());

        JsonNode n = notifications(owner);
        assertThat(types(n)).contains("INTEREST");
        JsonNode item = n.get("items").get(types(n).indexOf("INTEREST"));
        assertThat(item.get("payload").get("offerTitle").asText()).isEqualTo("nc ladder");
        assertThat(item.get("payload").get("fromUsername").asText()).isEqualTo("nc_buyer");
        assertThat(n.get("unread").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void newPostNotifiesSubscribers() throws Exception {
        AuthedUser author = signUp("nc_author");
        AuthedUser follower = signUp("nc_follower");

        mvc.perform(post("/api/subscriptions")
                        .header("X-Session-Token", follower.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subscriberId":"%s","subscribedToId":"%s"}
                                """.formatted(follower.id(), author.id())))
                .andExpect(status().is2xxSuccessful());

        createOffer(author, "nc followed offer");

        JsonNode n = notifications(follower);
        assertThat(types(n)).contains("NEW_POST_FROM_SUB");
        JsonNode item = n.get("items").get(types(n).indexOf("NEW_POST_FROM_SUB"));
        assertThat(item.get("payload").get("title").asText()).isEqualTo("nc followed offer");
    }

    @Test
    void offlineMessageCreatesANotificationAsync() throws Exception {
        AuthedUser sender = signUp("nc_sender");
        AuthedUser recipient = signUp("nc_recipient");

        mvc.perform(post("/api/messages")
                        .header("X-Session-Token", sender.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"senderId":"%s","recipientId":"%s","content":"hello offline"}
                                """.formatted(sender.id(), recipient.id())))
                .andExpect(status().is2xxSuccessful());

        // notification creation runs @Async — poll briefly
        boolean found = false;
        for (int i = 0; i < 50 && !found; i++) {
            found = types(notifications(recipient)).contains("NEW_MESSAGE");
            if (!found) Thread.sleep(100);
        }
        assertThat(found).as("NEW_MESSAGE notification within 5s").isTrue();
    }

    @Test
    void thanksNotifiesTheGiver() throws Exception {
        AuthedUser giver = signUp("nc_giver");
        AuthedUser taker = signUp("nc_taker");
        String offerId = createOffer(giver, "nc thanked");
        mvc.perform(post("/api/offers/" + offerId + "/interest")
                .header("X-Session-Token", taker.token())).andExpect(status().isOk());
        mvc.perform(post("/api/offers/" + offerId + "/status")
                        .header("X-Session-Token", giver.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GIVEN\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/offers/" + offerId + "/thanks")
                        .header("X-Session-Token", taker.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        assertThat(types(notifications(giver))).contains("THANKS");
    }

    @Test
    void markAllReadClearsTheUnreadCount() throws Exception {
        AuthedUser owner = signUp("nc_reader");
        AuthedUser buyer = signUp("nc_reader_buyer");
        String offerId = createOffer(owner, "nc read me");
        mvc.perform(post("/api/offers/" + offerId + "/interest")
                .header("X-Session-Token", buyer.token())).andExpect(status().isOk());

        assertThat(notifications(owner).get("unread").asLong()).isGreaterThanOrEqualTo(1);

        mvc.perform(post("/api/notifications/mark-all-read")
                        .header("X-Session-Token", owner.token()))
                .andExpect(status().isOk());

        JsonNode after = notifications(owner);
        assertThat(after.get("unread").asLong()).isZero();
        assertThat(after.get("items").get(0).get("readAt").asText()).isNotEmpty();
    }

    @Test
    void notificationsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        // the public unsubscribe endpoint must remain reachable without a token
        // (it serves an HTML page — anything but 401 proves the filter lets it through)
        mvc.perform(get("/api/notifications/unsubscribe").param("token", "nope"))
                .andExpect(status().isOk());
    }
}
