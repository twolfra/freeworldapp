package de.freeworldapp.app;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ownership checks: mutating someone else's data must return 403 (AP 0.4). */
class OwnershipIntegrationTest extends IntegrationTestBase {

    private static final String POST_BODY = """
            {"title":"Ladder","description":"Sturdy aluminium ladder","region":"Leipzig",
             "category":"Tools & Equipment","quantity":1}
            """;

    private static final String POST_UPDATE = """
            {"title":"Ladder (updated)","description":"Sturdy aluminium ladder","region":"Leipzig",
             "category":"Tools & Equipment","quantity":2}
            """;

    private String createPost(String basePath, AuthedUser owner) throws Exception {
        var result = mvc.perform(post(basePath)
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(POST_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }

    @Test
    void ownerCanUpdateOwnOffer() throws Exception {
        AuthedUser owner = signUp("offer_owner_ok");
        String id = createPost("/api/offers", owner);

        mvc.perform(put("/api/offers/" + id)
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(POST_UPDATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Ladder (updated)"));
    }

    @Test
    void nonOwnerCannotUpdateOffer() throws Exception {
        AuthedUser owner = signUp("offer_owner_u");
        AuthedUser intruder = signUp("offer_intruder_u");
        String id = createPost("/api/offers", owner);

        mvc.perform(put("/api/offers/" + id)
                        .header("X-Session-Token", intruder.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(POST_UPDATE))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwnerCannotDeleteOffer() throws Exception {
        AuthedUser owner = signUp("offer_owner_d");
        AuthedUser intruder = signUp("offer_intruder_d");
        String id = createPost("/api/offers", owner);

        mvc.perform(delete("/api/offers/" + id)
                        .header("X-Session-Token", intruder.token()))
                .andExpect(status().isForbidden());

        // owner still can
        mvc.perform(delete("/api/offers/" + id)
                        .header("X-Session-Token", owner.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonOwnerCannotUpdateRequest() throws Exception {
        AuthedUser owner = signUp("req_owner_u");
        AuthedUser intruder = signUp("req_intruder_u");
        String id = createPost("/api/requests", owner);

        mvc.perform(put("/api/requests/" + id)
                        .header("X-Session-Token", intruder.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(POST_UPDATE))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwnerCannotDeleteRequest() throws Exception {
        AuthedUser owner = signUp("req_owner_d");
        AuthedUser intruder = signUp("req_intruder_d");
        String id = createPost("/api/requests", owner);

        mvc.perform(delete("/api/requests/" + id)
                        .header("X-Session-Token", intruder.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotUpdateAnotherUsersAccount() throws Exception {
        AuthedUser victim = signUp("acct_victim_u");
        AuthedUser intruder = signUp("acct_intruder_u");

        mvc.perform(put("/api/users/" + victim.id())
                        .header("X-Session-Token", intruder.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"hijacked","email":"hijacked@example.com"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotDeleteAnotherUsersAccount() throws Exception {
        AuthedUser victim = signUp("acct_victim_d");
        AuthedUser intruder = signUp("acct_intruder_d");

        mvc.perform(delete("/api/users/" + victim.id())
                        .header("X-Session-Token", intruder.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminCannotUseAdminEndpoints() throws Exception {
        AuthedUser user = signUp("plain_user_admin");

        mvc.perform(post("/api/admin/users/" + user.id() + "/block")
                        .header("X-Session-Token", user.token()))
                .andExpect(status().isForbidden());
    }
}
