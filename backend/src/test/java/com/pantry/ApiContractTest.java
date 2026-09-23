package com.pantry;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ApiContractTest {
    @Test
    void reviewedContractCoversEveryControllerRouteAndProtocolGuard() throws IOException {
        String contract;
        try (var stream = getClass().getResourceAsStream("/openapi/pantry-v1.yaml")) {
            assertThat(stream).isNotNull();
            contract = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(contract)
                .contains("openapi: 3.1.0")
                .contains("/api/v1/csrf:")
                .contains("/api/v1/guest-sessions:")
                .contains("/api/v1/guest-sessions/current:")
                .contains("/api/v1/meal-plans:")
                .contains("/api/v1/meal-plans/{planId}:")
                .contains("/api/v1/meal-plans/{planId}/basket/lines/{lineId}:")
                .contains("/api/v1/meal-plans/{planId}/basket/lines/{lineId}/product:")
                .contains("/api/v1/meal-plans/{planId}/basket/refresh:")
                .contains("/api/v1/meal-plans/{planId}/meals/{slot}/replace:")
                .contains("/api/v1/meal-plans/{planId}/basket/finalize:")
                .contains("/api/v1/meal-plans/{planId}/basket/recovery-demo:")
                .contains("name: Idempotency-Key")
                .contains("name: PANTRY_GUEST")
                .contains("application/problem+json");
    }
}
