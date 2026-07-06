package de.freeworldapp.app;

import com.fasterxml.jackson.databind.JsonNode;
import de.freeworldapp.app.user.Role;
import de.freeworldapp.app.user.User;
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

/** AP 2.3: offer/request lifecycle — status changes, list filtering. */
class LifecycleIntegrationTest extends IntegrationTestBase {

    private String createOffer(AuthedUser owner, String title) throws Exception {
        MvcResult result = mvc.perform(post("/api/offers")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"desc","region":"Leipzig",
                                 "category":"Other","quantity":1}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private List<String> listTitles(String url) throws Exception {
        MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        JsonNode arr = objectMapper.readTree(result.getResponse().getContentAsString());
        List<String> titles = new ArrayList<>();
        arr.forEach(n -> titles.add(n.get("title").asText()));
        return titles;
    }

    @Test
    void newOffersAreActiveByDefault() throws Exception {
        AuthedUser owner = signUp("lc_default");
        String id = createOffer(owner, "lc default offer");
        mvc.perform(get("/api/offers/" + id))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void givenOffersDisappearFromTheDefaultListButNotFromProfileOrCompletedView() throws Exception {
        AuthedUser owner = signUp("lc_given");
        String id = createOffer(owner, "lc given offer");

        mvc.perform(post("/api/offers/" + id + "/status")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"GIVEN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GIVEN"));

        assertThat(listTitles("/api/offers")).doesNotContain("lc given offer");
        assertThat(listTitles("/api/offers?includeCompleted=true")).contains("lc given offer");
        assertThat(listTitles("/api/offers?offeredBy=" + owner.id())).contains("lc given offer");
    }

    @Test
    void reservingForAUserAndBackToActiveClearsTheReservation() throws Exception {
        AuthedUser owner = signUp("lc_reserver");
        AuthedUser lucky = signUp("lc_lucky");
        String id = createOffer(owner, "lc reserved offer");

        mvc.perform(post("/api/offers/" + id + "/status")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESERVED","reservedForId":"%s"}
                                """.formatted(lucky.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.reservedForUsername").value("lc_lucky"));

        // reserved offers stay in the default list (they are not gone yet)
        assertThat(listTitles("/api/offers")).contains("lc reserved offer");

        mvc.perform(post("/api/offers/" + id + "/status")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedForUsername").doesNotExist());
    }

    @Test
    void onlyOwnerOrAdminMayChangeStatus() throws Exception {
        AuthedUser owner = signUp("lc_owner");
        AuthedUser stranger = signUp("lc_stranger");
        String id = createOffer(owner, "lc protected offer");

        mvc.perform(post("/api/offers/" + id + "/status")
                        .header("X-Session-Token", stranger.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"GIVEN"}
                                """))
                .andExpect(status().isForbidden());

        // an admin may
        AuthedUser admin;
        register("lc_admin");
        verifyEmail("lc_admin");
        User u = userRepository.findByUsername("lc_admin").orElseThrow();
        u.setRole(Role.ADMIN);
        userRepository.save(u);
        admin = login("lc_admin");

        mvc.perform(post("/api/offers/" + id + "/status")
                        .header("X-Session-Token", admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"GIVEN"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void invalidStatusValueIsRejected() throws Exception {
        AuthedUser owner = signUp("lc_invalid");
        String id = createOffer(owner, "lc invalid offer");
        mvc.perform(post("/api/offers/" + id + "/status")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SOLD"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fulfilledRequestsAreHiddenFromTheDefaultList() throws Exception {
        AuthedUser owner = signUp("lc_requester");
        MvcResult result = mvc.perform(post("/api/requests")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"lc fulfilled request","description":"desc","region":"Leipzig",
                                 "category":"Other","quantity":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(post("/api/requests/" + id + "/status")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"FULFILLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));

        assertThat(listTitles("/api/requests")).doesNotContain("lc fulfilled request");
        assertThat(listTitles("/api/requests?includeCompleted=true")).contains("lc fulfilled request");
    }
}
