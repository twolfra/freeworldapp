package de.freeworldapp.app;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AP 2.4: interest flow creates a context-carrying first message. */
class InterestFlowIntegrationTest extends IntegrationTestBase {

    private String createOffer(AuthedUser owner) throws Exception {
        MvcResult result = mvc.perform(post("/api/offers")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Interest ladder","description":"desc","region":"Leipzig",
                                 "category":"Other","quantity":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void interestCreatesContextMessageAndConversation() throws Exception {
        AuthedUser owner = signUp("int_owner");
        AuthedUser buyer = signUp("int_buyer");
        String offerId = createOffer(owner);

        mvc.perform(post("/api/offers/" + offerId + "/interest")
                        .header("X-Session-Token", buyer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationWith").value(owner.id()))
                .andExpect(jsonPath("$.created").value(true));

        // the conversation now contains the structured message with context
        MvcResult conv = mvc.perform(get("/api/messages/conversation")
                        .param("userId", buyer.id()).param("otherId", owner.id())
                        .header("X-Session-Token", buyer.token()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode messages = objectMapper.readTree(conv.getResponse().getContentAsString());
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).get("content").asText()).contains("Interest ladder");
        assertThat(messages.get(0).get("contextType").asText()).isEqualTo("OFFER");
        assertThat(messages.get(0).get("contextId").asText()).isEqualTo(offerId);
    }

    @Test
    void interestIsIdempotentPerUserAndOffer() throws Exception {
        AuthedUser owner = signUp("int_owner2");
        AuthedUser buyer = signUp("int_buyer2");
        String offerId = createOffer(owner);

        mvc.perform(post("/api/offers/" + offerId + "/interest")
                        .header("X-Session-Token", buyer.token()))
                .andExpect(jsonPath("$.created").value(true));
        mvc.perform(post("/api/offers/" + offerId + "/interest")
                        .header("X-Session-Token", buyer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(false));

        MvcResult conv = mvc.perform(get("/api/messages/conversation")
                        .param("userId", buyer.id()).param("otherId", owner.id())
                        .header("X-Session-Token", buyer.token()))
                .andReturn();
        assertThat(objectMapper.readTree(conv.getResponse().getContentAsString())).hasSize(1);
    }

    @Test
    void ownInterestIsRejectedAndCountIsOwnerOnly() throws Exception {
        AuthedUser owner = signUp("int_owner3");
        AuthedUser buyer1 = signUp("int_buyer3");
        AuthedUser buyer2 = signUp("int_buyer4");
        String offerId = createOffer(owner);

        mvc.perform(post("/api/offers/" + offerId + "/interest")
                        .header("X-Session-Token", owner.token()))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/offers/" + offerId + "/interest")
                        .header("X-Session-Token", buyer1.token()));
        mvc.perform(post("/api/offers/" + offerId + "/interest")
                        .header("X-Session-Token", buyer2.token()));

        mvc.perform(get("/api/offers/" + offerId + "/interested")
                        .header("X-Session-Token", owner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        mvc.perform(get("/api/offers/" + offerId + "/interested")
                        .header("X-Session-Token", buyer1.token()))
                .andExpect(status().isForbidden());
    }
}
