package com.pantry.ai.internal;

import com.pantry.ai.MealProposalGateway;
import com.pantry.shared.Quantity.Unit;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Test/demo adapter for exercising the exact structured-output contract without a paid provider. */
@Component
@ConditionalOnProperty(name = "pantry.ai-adapter", havingValue = "fake")
public class DeterministicFakeAiGateway implements MealProposalGateway {
    @Override
    public ProposalBatch propose(ProposalRequest request) {
        List<MealProposal> meals = IntStream.range(0, request.dinnerCount())
                .mapToObj(index -> new MealProposal(
                        "Structured lentil skillet " + (index + 1),
                        "A deterministic fake proposal used for schema and safety testing.",
                        request.householdSize(),
                        Math.min(request.maxCookingMinutes() == null ? 30 : request.maxCookingMinutes(), 30),
                        Set.of(),
                        List.of(
                                new IngredientProposal("green-lentils", "Green lentils", new BigDecimal("400"), Unit.GRAM),
                                new IngredientProposal("onion", "Onion", new BigDecimal("100"), Unit.GRAM)),
                        List.of("Cook the lentils.", "Soften the onion and combine.")))
                .toList();
        return new ProposalBatch("1.0", "deterministic-fake", meals);
    }
}
