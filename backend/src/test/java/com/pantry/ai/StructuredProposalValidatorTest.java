package com.pantry.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pantry.ai.MealProposalGateway.IngredientProposal;
import com.pantry.ai.MealProposalGateway.MealProposal;
import com.pantry.ai.MealProposalGateway.ProposalBatch;
import com.pantry.ai.MealProposalGateway.ProposalRequest;
import com.pantry.ai.internal.DeterministicFakeAiGateway;
import com.pantry.shared.Quantity.Unit;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StructuredProposalValidatorTest {
    private final StructuredProposalValidator validator = new StructuredProposalValidator();

    @Test
    void acceptsACompleteTypedBatchWithTheRequestedMealCount() {
        ProposalRequest request = request(Set.of(), Set.of());
        ProposalBatch batch = new DeterministicFakeAiGateway().propose(request);

        assertThat(validator.validate(batch, request).meals()).hasSize(3);
    }

    @Test
    void rejectsAllergenConflictsEvenWhenTheProviderReturnsValidJson() {
        ProposalRequest request = request(Set.of("SOY"), Set.of());
        MealProposal meal = meal("Unsafe tofu", Set.of("SOY"), "tofu");
        ProposalBatch batch = new ProposalBatch("1.0", "test", List.of(meal, meal("Two", Set.of(), "rice"),
                meal("Three", Set.of(), "onion")));

        assertThatThrownBy(() -> validator.validate(batch, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allergen");
    }

    @Test
    void rejectsExcludedIngredientsAndIncorrectMealCounts() {
        ProposalRequest request = request(Set.of(), Set.of("mushrooms"));
        ProposalBatch excluded = new ProposalBatch("1.0", "test", List.of(
                meal("One", Set.of(), "mushrooms"), meal("Two", Set.of(), "rice"), meal("Three", Set.of(), "onion")));

        assertThatThrownBy(() -> validator.validate(excluded, request)).hasMessageContaining("excluded");
        assertThatThrownBy(() -> validator.validate(new DeterministicFakeAiGateway().propose(
                new ProposalRequest(4, 2, 30, Set.of(), Set.of(), Set.of())), request))
                .hasMessageContaining("meal count");
    }

    private ProposalRequest request(Set<String> allergens, Set<String> excluded) {
        return new ProposalRequest(4, 3, 30, Set.of("PLANT_BASED"), allergens, excluded);
    }

    private MealProposal meal(String title, Set<String> allergens, String ingredient) {
        return new MealProposal(title, "Test meal", 4, 20, allergens,
                List.of(new IngredientProposal(ingredient, ingredient, new BigDecimal("100"), Unit.GRAM)),
                List.of("Cook safely."));
    }
}
