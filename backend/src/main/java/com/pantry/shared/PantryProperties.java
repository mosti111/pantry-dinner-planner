package com.pantry.shared;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("pantry")
public record PantryProperties(String allowedOrigin, boolean demoMode, boolean secureCookie, Duration guestSessionTtl) {
}
