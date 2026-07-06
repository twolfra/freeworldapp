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

/** AP 3.1 + 3.5: PLZ geocoding, radius search, full-text search, combined filters. */
class GeoSearchIntegrationTest extends IntegrationTestBase {

    private String createOffer(AuthedUser owner, String title, String plz, String category, String imageUrl) throws Exception {
        String imagePart = imageUrl == null ? "" : ",\"imageUrl\":\"" + imageUrl + "\"";
        MvcResult result = mvc.perform(post("/api/offers")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"desc for %s","region":"ignored",
                                 "category":"%s","quantity":1,"postalCode":"%s"%s}
                                """.formatted(title, title, category, plz, imagePart)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private JsonNode search(String query) throws Exception {
        MvcResult result = mvc.perform(get("/api/search" + query)).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private List<String> titles(JsonNode response) {
        List<String> out = new ArrayList<>();
        response.get("items").forEach(n -> out.add(n.get("title").asText()));
        return out;
    }

    @Test
    void postalCodeResolvesToCityAndCentroid() throws Exception {
        AuthedUser user = signUp("geo_create");
        MvcResult result = mvc.perform(post("/api/offers")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"geo ladder","description":"d","region":"whatever",
                                 "category":"Other","quantity":1,"postalCode":"04315"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.city").value("Leipzig"))
                .andExpect(jsonPath("$.postalCode").value("04315"))
                .andExpect(jsonPath("$.region").value("04315 Leipzig"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("lat").asDouble()).isBetween(51.0, 51.7);
        assertThat(body.get("lon").asDouble()).isBetween(12.0, 12.8);
    }

    @Test
    void updateCanSetOrChangeThePostalCode() throws Exception {
        AuthedUser user = signUp("geo_update");
        String id = createOffer(user, "geo update me", "04315", "Other", null);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/offers/" + id)
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"geo update me","description":"d","region":"x",
                                 "category":"Other","quantity":1,"postalCode":"10115"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Berlin"))
                .andExpect(jsonPath("$.postalCode").value("10115"));
    }

    @Test
    void unknownPostalCodeIsRejected() throws Exception {
        AuthedUser user = signUp("geo_unknown");
        mvc.perform(post("/api/offers")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"x","description":"d","region":"r",
                                 "category":"Other","quantity":1,"postalCode":"00000"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postalAutocompleteFindsByPlzAndCityPrefix() throws Exception {
        mvc.perform(get("/api/geo/postal").param("q", "0431"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].plz").value(org.hamcrest.Matchers.startsWith("0431")));

        mvc.perform(get("/api/geo/postal").param("q", "Leipz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Leipzig"));
    }

    @Test
    void radiusSearchFiltersAndSortsByDistance() throws Exception {
        AuthedUser user = signUp("geo_radius");
        createOffer(user, "geo in leipzig", "04315", "Other", null);
        createOffer(user, "geo in halle", "06108", "Other", null);   // ~35 km from Leipzig
        createOffer(user, "geo in berlin", "10115", "Other", null);  // ~150 km

        // Leipzig centre, 60 km: Leipzig + Halle, not Berlin; nearest first
        JsonNode r = search("?type=offers&lat=51.34&lon=12.37&radiusKm=60&sort=nearest&size=50");
        List<String> t = titles(r);
        assertThat(t).contains("geo in leipzig", "geo in halle").doesNotContain("geo in berlin");
        assertThat(t.indexOf("geo in leipzig")).isLessThan(t.indexOf("geo in halle"));

        JsonNode first = r.get("items").get(t.indexOf("geo in leipzig"));
        assertThat(first.get("distanceKm").asDouble()).isLessThan(10.0);
    }

    @Test
    void fullTextSearchMatchesGermanContent() throws Exception {
        AuthedUser user = signUp("geo_fts");
        createOffer(user, "Gebrauchtes Fahrrad abzugeben", "04315", "Transport", null);
        createOffer(user, "Alte Stühle", "04315", "Furniture", null);

        JsonNode r = search("?q=Fahrrad&size=50");
        assertThat(titles(r)).contains("Gebrauchtes Fahrrad abzugeben")
                .doesNotContain("Alte Stühle");
    }

    @Test
    void filtersCombineAndCompletedPostsAreHidden() throws Exception {
        AuthedUser user = signUp("geo_combi");
        createOffer(user, "combi book with image", "04315", "Books & Media", "/api/images/x.jpg");
        createOffer(user, "combi book plain", "04315", "Books & Media", null);
        String givenId = createOffer(user, "combi given away", "04315", "Books & Media", null);
        mvc.perform(post("/api/offers/" + givenId + "/status")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GIVEN\"}"))
                .andExpect(status().isOk());

        MvcResult combined = mvc.perform(get("/api/search")
                        .param("q", "combi").param("category", "Books & Media")
                        .param("withImage", "true").param("size", "50"))
                .andExpect(status().isOk()).andReturn();
        JsonNode r = objectMapper.readTree(combined.getResponse().getContentAsString());
        assertThat(titles(r)).containsExactly("combi book with image");

        JsonNode all = search("?q=combi&size=50");
        assertThat(titles(all)).contains("combi book with image", "combi book plain")
                .doesNotContain("combi given away");

        JsonNode withCompleted = search("?q=combi&includeCompleted=true&size=50");
        assertThat(titles(withCompleted)).contains("combi given away");
    }

    @Test
    void paginationReturnsTotalAndRespectsSize() throws Exception {
        AuthedUser user = signUp("geo_page");
        for (int i = 0; i < 5; i++) createOffer(user, "page offer " + i, "04315", "Other", null);

        JsonNode r = search("?q=page offer&size=2&page=0");
        assertThat(r.get("items")).hasSize(2);
        assertThat(r.get("total").asLong()).isGreaterThanOrEqualTo(5);
        JsonNode r2 = search("?q=page offer&size=2&page=1");
        assertThat(titles(r2)).doesNotContainAnyElementsOf(titles(r));
    }
}
