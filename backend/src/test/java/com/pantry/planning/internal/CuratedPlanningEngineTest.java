package com.pantry.planning.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pantry.planning.PlanView;
import com.pantry.ingredient.IngredientNormalizer;
import com.pantry.recipe.internal.ReferenceRecipeCatalog;
import com.pantry.retailer.internal.ReferenceRetailerGateway;
import com.pantry.planning.PlanningRequest;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CuratedPlanningEngineTest {
    private final CuratedPlanningEngine engine = new CuratedPlanningEngine(
            new IngredientNormalizer(), new ReferenceRecipeCatalog(), new ReferenceRetailerGateway());
    private final Instant now = Instant.parse("2026-09-21T12:00:00Z");

    @Test
    void createsWholePackageBasketWithinBudget() {
        PlanView plan = engine.create(UUID.randomUUID(), request(), now);

        assertThat(plan.meals()).hasSize(3);
        assertThat(plan.basket().lines()).isNotEmpty();
        assertThat(plan.basket().lines()).allSatisfy(line -> {
            assertThat(line.packageCount()).isPositive();
            assertThat(line.sku()).isNotBlank();
            assertThat(line.alternatives()).isNotEmpty();
        });
        assertThat(Double.parseDouble(plan.total().amount()))
                .isLessThanOrEqualTo(Double.parseDouble(plan.budget().amount()));
    }

    @Test
    void replacesSkuOnlyWithApprovedAlternative() {
        PlanView plan = engine.create(UUID.randomUUID(), request(), now);
        PlanView.BasketLineView line = plan.basket().lines().getFirst();

        PlanView changed = engine.selectProduct(plan, line.id(), line.alternatives().getFirst().sku(), now.plusSeconds(1));

        assertThat(changed.basket().lines().getFirst().sku()).endsWith("-ALT");
        assertThat(changed.version()).isEqualTo(1);
        assertThatThrownBy(() -> engine.selectProduct(plan, line.id(), "INVENTED-SKU", now))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replacesAffectedMealAndRecalculatesBasket() {
        PlanView plan = engine.create(UUID.randomUUID(), request(), now);
        String original = plan.meals().getFirst().title();

        PlanView changed = engine.replaceMeal(plan, request(), plan.meals().getFirst().slot(), now.plusSeconds(1));

        assertThat(changed.meals().getFirst().title()).isNotEqualTo(original);
        assertThat(changed.basket().status()).isEqualTo("NEEDS_REVIEW");
        assertThat(changed.notices().getLast()).contains("Recovery level 3");
    }

    @Test
    void selectsLowerCostApprovedProductsBeforeRejectingBudget() {
        PlanView baseline = engine.create(UUID.randomUUID(), request(), now);
        String tighterBudget = new BigDecimal(baseline.total().amount()).subtract(new BigDecimal("1.00")).toPlainString();
        PlanningRequest constrained = new PlanningRequest(4, 3,
                new PlanningRequest.MoneyInput(tighterBudget, "TRY"), null,
                PlanningRequest.Preference.BALANCED, Set.of(), Set.of(), Set.of(), true);

        PlanView optimized = engine.create(UUID.randomUUID(), constrained, now);

        assertThat(new BigDecimal(optimized.total().amount()))
                .isLessThanOrEqualTo(new BigDecimal(tighterBudget));
        assertThat(optimized.notices()).anyMatch(notice -> notice.contains("Lower-cost"));
    }

    @Test
    void recoversUnavailableIngredientThroughApprovedSubstitution() {
        PlanView plan = engine.create(UUID.randomUUID(), request(), now);

        PlanView recovered = engine.simulateRecovery(plan, request(), "INGREDIENT_UNAVAILABLE", now.plusSeconds(1));

        assertThat(recovered.notices().getLast()).contains("Recovery level 2");
        assertThat(recovered.basket().status()).isEqualTo("RECOVERED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SKU_UNAVAILABLE", "INGREDIENT_UNAVAILABLE", "PRICE_INCREASE",
            "PROMOTIONS_EXPIRED", "RETAILER_UNAVAILABLE", "CART_TRANSFER_FAILURE"})
    void everySupportedFailureScenarioReturnsAReviewableRecovery(String scenario) {
        PlanView plan = engine.create(UUID.randomUUID(), request(), now);

        PlanView recovered = engine.simulateRecovery(plan, request(), scenario, now.plusSeconds(1));

        assertThat(recovered.version()).isGreaterThan(plan.version());
        assertThat(recovered.notices()).isNotEmpty();
        assertThat(recovered.basket().lines()).isNotEmpty();
    }

    @Test
    void rejectsUnsafeRecipeShortage() {
        PlanningRequest impossible = new PlanningRequest(4, 5,
                new PlanningRequest.MoneyInput("1500.00", "TRY"), 30,
                PlanningRequest.Preference.BALANCED, Set.of("GLUTEN_FREE"), Set.of("SOY"),
                Set.of("green-lentils", "chickpeas"), true);

        assertThatThrownBy(() -> engine.create(UUID.randomUUID(), impossible, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("safe recipes");
    }

    private PlanningRequest request() {
        return new PlanningRequest(4, 3, new PlanningRequest.MoneyInput("1500.00", "TRY"),
                null, PlanningRequest.Preference.BALANCED, Set.of(), Set.of(), Set.of(), true);
    }
}
