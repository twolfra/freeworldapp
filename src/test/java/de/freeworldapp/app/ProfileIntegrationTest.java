package de.freeworldapp.app;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AP 2.6: optional profile fields; postal code and email never public. */
class ProfileIntegrationTest extends IntegrationTestBase {

    @Test
    void ownerUpdatesProfileAndPublicViewHidesPostalCodeAndEmail() throws Exception {
        AuthedUser user = signUp("prof_owner");

        mvc.perform(put("/api/users/" + user.id() + "/profile")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Timo","bio":"I give things away.",
                                 "postalCode":"04315","city":"Leipzig"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Timo"))
                .andExpect(jsonPath("$.postalCode").value("04315"));

        mvc.perform(get("/api/users/" + user.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Timo"))
                .andExpect(jsonPath("$.bio").value("I give things away."))
                .andExpect(jsonPath("$.city").value("Leipzig"))
                .andExpect(jsonPath("$.postalCode").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void nonOwnerCannotUpdateSomeoneElsesProfile() throws Exception {
        AuthedUser victim = signUp("prof_victim");
        AuthedUser intruder = signUp("prof_intruder");

        mvc.perform(put("/api/users/" + victim.id() + "/profile")
                        .header("X-Session-Token", intruder.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"hacked"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void nullKeepsAndEmptyStringClearsAField() throws Exception {
        AuthedUser user = signUp("prof_partial");

        mvc.perform(put("/api/users/" + user.id() + "/profile")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Keep Me","city":"Leipzig"}
                                """))
                .andExpect(status().isOk());

        // city omitted (null) → kept; displayName "" → cleared
        mvc.perform(put("/api/users/" + user.id() + "/profile")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").doesNotExist())
                .andExpect(jsonPath("$.city").value("Leipzig"));
    }

    @Test
    void tooLongFieldsAreRejected() throws Exception {
        AuthedUser user = signUp("prof_long");
        mvc.perform(put("/api/users/" + user.id() + "/profile")
                        .header("X-Session-Token", user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"%s"}
                                """.formatted("x".repeat(61))))
                .andExpect(status().isBadRequest());
    }
}
