package de.freeworldapp.app;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AP 3.3: ordered multi-image galleries, first image = cover. */
class PostImagesIntegrationTest extends IntegrationTestBase {

    private String createOffer(AuthedUser owner) throws Exception {
        MvcResult result = mvc.perform(post("/api/offers")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"gallery ladder","description":"d","region":"Leipzig",
                                 "category":"Other","quantity":1}
                                """))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String imagesBody(int n) {
        StringBuilder sb = new StringBuilder("{\"images\":[");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"url\":\"/api/images/g").append(i)
              .append(".jpg\",\"thumbUrl\":\"/api/images/g").append(i).append("_t.jpg\"}");
        }
        return sb.append("]}").toString();
    }

    @Test
    void galleryIsOrderedAndFirstImageBecomesCover() throws Exception {
        AuthedUser owner = signUp("gal_owner");
        String id = createOffer(owner);

        mvc.perform(put("/api/offers/" + id + "/images")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imagesBody(3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(3));

        mvc.perform(get("/api/offers/" + id))
                .andExpect(jsonPath("$.imageUrl").value("/api/images/g0.jpg"))
                .andExpect(jsonPath("$.images[0].url").value("/api/images/g0.jpg"))
                .andExpect(jsonPath("$.images[2].sortOrder").value(2));

        // reorder: last becomes first → cover follows
        mvc.perform(put("/api/offers/" + id + "/images")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"images":[{"url":"/api/images/g2.jpg","thumbUrl":"/api/images/g2_t.jpg"},
                                           {"url":"/api/images/g0.jpg","thumbUrl":"/api/images/g0_t.jpg"}]}
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/api/offers/" + id))
                .andExpect(jsonPath("$.imageUrl").value("/api/images/g2.jpg"))
                .andExpect(jsonPath("$.images.length()").value(2));
    }

    @Test
    void moreThanFiveImagesAreRejected() throws Exception {
        AuthedUser owner = signUp("gal_max");
        String id = createOffer(owner);
        mvc.perform(put("/api/offers/" + id + "/images")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imagesBody(6)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyTheOwnerManagesTheGallery() throws Exception {
        AuthedUser owner = signUp("gal_victim");
        AuthedUser intruder = signUp("gal_intruder");
        String id = createOffer(owner);
        mvc.perform(put("/api/offers/" + id + "/images")
                        .header("X-Session-Token", intruder.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imagesBody(1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void clearingTheGalleryClearsTheCover() throws Exception {
        AuthedUser owner = signUp("gal_clear");
        String id = createOffer(owner);
        mvc.perform(put("/api/offers/" + id + "/images")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imagesBody(2)))
                .andExpect(status().isOk());
        mvc.perform(put("/api/offers/" + id + "/images")
                        .header("X-Session-Token", owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"images\":[]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/offers/" + id))
                .andExpect(jsonPath("$.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.images.length()").value(0));
    }
}
