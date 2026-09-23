package com.pantry.planning.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PlanTransitionRepository extends JpaRepository<PlanTransitionEntity, UUID> {}
