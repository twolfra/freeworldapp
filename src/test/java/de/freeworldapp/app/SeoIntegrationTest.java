package de.freeworldapp.app;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AP 3.6: sitemap, robots.txt, server-side OG-tag injection. */
class SeoIntegrationTest extends IntegrationTestBase {

    private String createOffer(AuthedUser owner, String title) throws Exception {
        MvcResult result = mvc.perform(post("/api/offers")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"A very nice <thing> to share & enjoy","region":"Leipzig",
                                 "category":"Other","quantity":1}
                                """.formatted(title)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void sitemapListsActivePostsWithSlugsButNotCompletedOnes() throws Exception {
        AuthedUser user = signUp("seo_sitemap");
        String activeId = createOffer(user, "Grüne Gießkanne für alle");
        String givenId = createOffer(user, "Schon vergebenes Teil");
        mvc.perform(post("/api/offers/" + givenId + "/status")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GIVEN\"}"))
                .andExpect(status().isOk());

        MvcResult r = mvc.perform(get("/sitemap.xml")).andExpect(status().isOk()).andReturn();
        String xml = r.getResponse().getContentAsString();
        assertThat(xml).contains("/offers/" + activeId + "/grune-giesskanne-fur-alle");
        assertThat(xml).doesNotContain(givenId);
        assertThat(xml).contains("/impressum").contains("<urlset");
    }

    @Test
    void robotsTxtDisallowsPrivateAreasAndPointsToSitemap() throws Exception {
        mvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Disallow: /api/")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sitemap: ")));
    }

    @Test
    void detailUrlServesHtmlWithInjectedOgTags() throws Exception {
        AuthedUser user = signUp("seo_og");
        String id = createOffer(user, "OG Testsessel");

        MvcResult r = mvc.perform(get("/offers/" + id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn();
        String html = r.getResponse().getContentAsString();
        assertThat(html).contains("<title>OG Testsessel — FreeWorld</title>");
        assertThat(html).contains("og:title").contains("OG Testsessel");
        // HTML-escaping of the description
        assertThat(html).contains("&lt;thing&gt;").doesNotContain("<thing>");
        assertThat(html).contains("og:url");

        // slugged URL works the same
        mvc.perform(get("/offers/" + id + "/og-testsessel"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("og:title")));
    }

    @Test
    void unknownDetailIdFallsThroughToTheSpa() throws Exception {
        // filter passes through; the SPA fallback (or 404 handling) takes over —
        // just assert we don't get an injected og:title for a random id
        MvcResult r = mvc.perform(get("/offers/00000000-0000-0000-0000-000000000000"))
                .andReturn();
        assertThat(r.getResponse().getContentAsString()).doesNotContain("og:title");
    }
}
