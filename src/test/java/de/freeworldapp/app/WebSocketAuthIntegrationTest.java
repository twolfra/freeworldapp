package de.freeworldapp.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import de.freeworldapp.app.user.User;
import de.freeworldapp.app.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AP 1.6: WebSocket authentication happens via the first frame
 * ({"type":"auth","token":...}) instead of a query parameter.
 * Needs a real server, hence RANDOM_PORT and its own Spring context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.ws.auth-timeout-ms=750")
class WebSocketAuthIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static { POSTGRES.start(); }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    int port;

    @Autowired TestRestTemplate rest;
    @Autowired UserRepository userRepository;
    @Autowired ObjectMapper objectMapper;

    private final List<WebSocketSession> openSessions = new ArrayList<>();

    @AfterEach
    void closeAll() {
        for (WebSocketSession s : openSessions) {
            try { s.close(); } catch (Exception ignored) {}
        }
        openSessions.clear();
    }

    /** Collects received frames and signals on close. */
    static class Probe extends TextWebSocketHandler {
        final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        final CountDownLatch closed = new CountDownLatch(1);
        volatile CloseStatus closeStatus;

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            frames.add(message.getPayload());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            this.closeStatus = status;
            closed.countDown();
        }
    }

    private String signUpAndLogin(String username) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Forwarded-For", "10.42.42.42");

        rest.postForEntity("/api/users", new HttpEntity<>("""
                {"username":"%s","email":"%s@example.com","password":"password123"}
                """.formatted(username, username), headers), String.class);

        User u = userRepository.findByUsername(username).orElseThrow();
        rest.getForEntity("/api/auth/verify?token=" + u.getVerificationToken(), String.class);

        var login = rest.postForEntity("/api/auth/login", new HttpEntity<>("""
                {"username":"%s","password":"password123"}
                """.formatted(username), headers), String.class);
        JsonNode body = objectMapper.readTree(login.getBody());
        return body.get("token").asText();
    }

    private WebSocketSession connect(Probe probe) throws Exception {
        WebSocketSession session = new StandardWebSocketClient()
                .execute(probe, "ws://localhost:" + port + "/ws/messages")
                .get(5, TimeUnit.SECONDS);
        openSessions.add(session);
        return session;
    }

    @Test
    void connectionWithoutAuthFrameIsClosedAfterTimeout() throws Exception {
        Probe probe = new Probe();
        connect(probe);
        assertThat(probe.closed.await(5, TimeUnit.SECONDS))
                .as("server closes unauthenticated connection after the timeout")
                .isTrue();
        assertThat(probe.closeStatus.getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
    }

    @Test
    void invalidTokenClosesTheConnectionImmediately() throws Exception {
        Probe probe = new Probe();
        WebSocketSession session = connect(probe);
        session.sendMessage(new TextMessage("""
                {"type":"auth","token":"garbage-token"}
                """));
        assertThat(probe.closed.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(probe.closeStatus.getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
    }

    @Test
    void tokenInQueryParamNoLongerAuthenticates() throws Exception {
        String token = signUpAndLogin("ws_query_" + UUID.randomUUID().toString().substring(0, 8));
        Probe probe = new Probe();
        WebSocketSession session = new StandardWebSocketClient()
                .execute(probe, "ws://localhost:" + port + "/ws/messages?token=" + token)
                .get(5, TimeUnit.SECONDS);
        openSessions.add(session);
        // no auth frame sent → closed despite the (ignored) query param
        assertThat(probe.closed.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(probe.closeStatus.getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
    }

    @Test
    void validAuthFrameEnablesChatBetweenTwoUsers() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String aliceToken = signUpAndLogin("ws_alice_" + suffix);
        String bobToken = signUpAndLogin("ws_bob_" + suffix);
        UUID bobId = userRepository.findByUsername("ws_bob_" + suffix).orElseThrow().getId();

        Probe alice = new Probe();
        WebSocketSession aliceWs = connect(alice);
        aliceWs.sendMessage(new TextMessage("""
                {"type":"auth","token":"%s"}
                """.formatted(aliceToken)));
        assertThat(alice.frames.poll(5, TimeUnit.SECONDS)).contains("auth_ok");

        Probe bob = new Probe();
        WebSocketSession bobWs = connect(bob);
        bobWs.sendMessage(new TextMessage("""
                {"type":"auth","token":"%s"}
                """.formatted(bobToken)));
        assertThat(bob.frames.poll(5, TimeUnit.SECONDS)).contains("auth_ok");

        aliceWs.sendMessage(new TextMessage("""
                {"type":"send","recipientId":"%s","content":"hello over hardened ws"}
                """.formatted(bobId)));

        String received = bob.frames.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull().contains("hello over hardened ws");
        JsonNode frame = objectMapper.readTree(received);
        assertThat(frame.get("type").asText()).isEqualTo("message");
        assertThat(frame.get("senderUsername").asText()).isEqualTo("ws_alice_" + suffix);
    }
}
