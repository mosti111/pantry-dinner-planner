package com.pantry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pantry.identity.GuestSessionService;
import com.pantry.planning.PlanNotFoundException;
import com.pantry.planning.PlanService;
import com.pantry.planning.PlanningRequest;
import com.pantry.support.RealPostgres;
import java.util.Set;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PantryPostgresIntegrationTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", RealPostgres::jdbcUrl);
        properties.add("spring.datasource.username", RealPostgres::username);
        properties.add("spring.datasource.password", RealPostgres::password);
        properties.add("pantry.allowed-origin", () -> "http://localhost:3000");
        properties.add("pantry.secure-cookie", () -> false);
        properties.add("pantry.guest-session-ttl", () -> "PT1H");
        properties.add("management.tracing.export.otlp.enabled", () -> false);
    }

    @Autowired GuestSessionService guestSessions;
    @Autowired PlanService plans;
    @Autowired ObjectMapper objectMapper;
    @LocalServerPort int port;

    @Test
    void persistsReloadsOwnsAndIdempotentlyFinalizesAPlan() {
        var owner = guestSessions.create();
        var other = guestSessions.create();
        var created = plans.create(owner.session(), request(), "integration-plan");

        assertThat(plans.get(owner.session(), created.id()).id()).isEqualTo(created.id());
        assertThatThrownBy(() -> plans.get(other.session(), created.id())).isInstanceOf(PlanNotFoundException.class);

        var cart = plans.finalizeCart(owner.session(), created.id(), "integration-cart");
        var replay = plans.finalizeCart(owner.session(), created.id(), "integration-cart");
        assertThat(replay.basket().cartReference()).isEqualTo(cart.basket().cartReference());
    }

    @Test
    void browserMutationRequiresCsrfAndIssuesAnHttpOnlyGuestCookie() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        URI guestUri = URI.create("http://localhost:" + port + "/api/v1/guest-sessions");
        HttpResponse<String> rejected = client.send(HttpRequest.newBuilder(guestUri)
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .header("Content-Type", "application/json").build(), HttpResponse.BodyHandlers.ofString());
        assertThat(rejected.statusCode()).isEqualTo(403);

        HttpResponse<String> csrf = client.send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/api/v1/csrf")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        String token = objectMapper.readTree(csrf.body()).get("token").asText();
        String csrfCookie = csrf.headers().allValues("set-cookie").stream()
                .map(value -> value.substring(0, value.indexOf(';')))
                .filter(value -> value.startsWith("XSRF-TOKEN="))
                .findFirst().orElseThrow();
        HttpResponse<String> created = client.send(HttpRequest.newBuilder(guestUri)
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .header("Content-Type", "application/json")
                .header("Origin", "http://localhost:3000")
                .header("Cookie", csrfCookie)
                .header("X-XSRF-TOKEN", token).build(), HttpResponse.BodyHandlers.ofString());

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.headers().allValues("set-cookie"))
                .anyMatch(value -> value.startsWith("PANTRY_GUEST=") && value.contains("HttpOnly"));
        assertThat(created.headers().firstValue("access-control-allow-origin"))
                .contains("http://localhost:3000");
    }

    @Test
    void missingGuestCookieReturnsTheStableErrorContractUsedByFirstRunOnboarding() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/guest-sessions/current"))
                .header("Origin", "http://localhost:3000")
                .GET().build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(objectMapper.readTree(response.body()).get("code").asText())
                .isEqualTo("GUEST_SESSION_REQUIRED");
        assertThat(response.headers().firstValue("access-control-allow-origin"))
                .contains("http://localhost:3000");
    }

    private PlanningRequest request() {
        return new PlanningRequest(4, 3, new PlanningRequest.MoneyInput("1500.00", "TRY"), 45,
                PlanningRequest.Preference.BALANCED, Set.of("VEGAN"), Set.of(), Set.of(), true);
    }
}
