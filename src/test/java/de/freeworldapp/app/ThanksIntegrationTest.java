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

/** AP 2.5: one qualitative thanks per completed gift, no scores. */
class ThanksIntegrationTest extends IntegrationTestBase {

    private String createOffer(AuthedUser owner, String title) throws Exception {
        MvcResult result = mvc.perform(post("/api/offers")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"desc","region":"Leipzig",
                                 "category":"Other","quantity":1}
                                """.formatted(title)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void setStatus(AuthedUser owner, String offerId, String status) throws Exception {
        mvc.perform(post("/api/offers/" + offerId + "/status")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    private void expressInterest(AuthedUser user, String offerId) throws Exception {
        mvc.perform(post("/api/offers/" + offerId + "/interest")
                        .header("X-Session-Token", user.token()))
                .andExpect(status().isOk());
    }

    @Test
    void thanksAfterGivenAppearsOnGiversProfile() throws Exception {
        AuthedUser giver = signUp("th_giver");
        AuthedUser taker = signUp("th_taker");
        String offerId = createOffer(giver, "th ladder");
        expressInterest(taker, offerId); // creates the conversation
        setStatus(giver, offerId, "GIVEN");

        mvc.perform(post("/api/offers/" + offerId + "/thanks")
                        .header("X-Session-Token", taker.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Danke, die Leiter ist super!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromUsername").value("th_taker"))
                .andExpect(jsonPath("$.offerTitle").value("th ladder"));

        MvcResult profile = mvc.perform(get("/api/users/" + giver.id() + "/thanks"))
                .andExpect(status().isOk()).andReturn();
        JsonNode list = objectMapper.readTree(profile.getResponse().getContentAsString());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("text").asText()).contains("Leiter");
    }

    @Test
    void onlyOneThanksPerGift() throws Exception {
        AuthedUser giver = signUp("th_giver2");
        AuthedUser taker = signUp("th_taker2");
        String offerId = createOffer(giver, "th one");
        expressInterest(taker, offerId);
        setStatus(giver, offerId, "GIVEN");

        mvc.perform(post("/api/offers/" + offerId + "/thanks")
                        .header("X-Session-Token", taker.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/offers/" + offerId + "/thanks")
                        .header("X-Session-Token", taker.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void thanksRequiresGivenStatus() throws Exception {
        AuthedUser giver = signUp("th_giver3");
        AuthedUser taker = signUp("th_taker3");
        String offerId = createOffer(giver, "th active");
        expressInterest(taker, offerId);

        mvc.perform(post("/api/offers/" + offerId + "/thanks")
                        .header("X-Session-Token", taker.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void thanksRequiresAConversationWithTheGiver() throws Exception {
        AuthedUser giver = signUp("th_giver4");
        AuthedUser stranger = signUp("th_stranger");
        String offerId = createOffer(giver, "th silent");
        setStatus(giver, offerId, "GIVEN");

        mvc.perform(post("/api/offers/" + offerId + "/thanks")
                        .header("X-Session-Token", stranger.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void selfThanksIsRejected() throws Exception {
        AuthedUser giver = signUp("th_giver5");
        String offerId = createOffer(giver, "th self");
        setStatus(giver, offerId, "GIVEN");

        mvc.perform(post("/api/offers/" + offerId + "/thanks")
                        .header("X-Session-Token", giver.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aThanksCanBeReported() throws Exception {
        AuthedUser giver = signUp("th_giver6");
        AuthedUser taker = signUp("th_taker6");
        String offerId = createOffer(giver, "th reported");
        expressInterest(taker, offerId);
        setStatus(giver, offerId, "GIVEN");

        MvcResult created = mvc.perform(post("/api/offers/" + offerId + "/thanks")
                        .header("X-Session-Token", taker.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"spam spam spam"}
                                """))
                .andExpect(status().isOk()).andReturn();
        String thanksId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        // the giver reports the thanks left on their gift
        mvc.perform(post("/api/reports")
                        .header("X-Session-Token", giver.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"THANKS","targetId":"%s","reason":"SPAM"}
                                """.formatted(thanksId)))
                .andExpect(status().isOk());
    }
}
