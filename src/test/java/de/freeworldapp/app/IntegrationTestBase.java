package de.freeworldapp.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.freeworldapp.app.user.User;
import de.freeworldapp.app.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full application (all filters included) against a shared
 * Testcontainers PostgreSQL instance. Flyway migrates the container schema,
 * so these tests also validate V1__baseline.sql against the entity model.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    // Singleton container shared by all test classes (single Spring context).
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;

    /** Distinct source IP per test instance so the per-IP rate limiter never interferes. */
    protected final String clientIp = "10.%d.%d.%d".formatted(
            ThreadLocalRandom.current().nextInt(1, 255),
            ThreadLocalRandom.current().nextInt(1, 255),
            ThreadLocalRandom.current().nextInt(1, 255));

    protected record AuthedUser(String id, String username, String token) {}

    protected void register(String username) throws Exception {
        mvc.perform(post("/api/users")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.com","password":"password123"}
                                """.formatted(username, username)))
                .andExpect(status().isCreated());
    }

    protected void verifyEmail(String username) throws Exception {
        User u = userRepository.findByUsername(username).orElseThrow();
        mvc.perform(get("/api/auth/verify").param("token", u.getVerificationToken()))
                .andExpect(status().isOk());
    }

    protected AuthedUser login(String username) throws Exception {
        var result = mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password123"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthedUser(body.get("id").asText(), username, body.get("token").asText());
    }

    /** register + verify + login in one step */
    protected AuthedUser signUp(String username) throws Exception {
        register(username);
        verifyEmail(username);
        return login(username);
    }
}
