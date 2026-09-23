package com.pantry.identity.internal;

import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class GuestSessionCleanupJob {
    private final GuestSessionRepository repository;
    private final Clock clock = Clock.systemUTC();

    GuestSessionCleanupJob(GuestSessionRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelayString = "${pantry.guest-cleanup-delay:PT1H}")
    @Transactional
    void purgeExpiredSessions() {
        repository.deleteExpiredBefore(clock.instant());
    }
}
