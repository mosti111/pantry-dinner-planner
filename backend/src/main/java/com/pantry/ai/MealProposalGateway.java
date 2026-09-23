package com.pantry.ai;

import com.pantry.shared.Quantity.Unit;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Provider-neutral boundary. Implementations return untrusted structured proposals;
 * they never return SKUs, prices, stock, package counts, totals, or cart outcomes.
 */
public interface MealProposalGateway {
    ProposalBatch propose(ProposalRequest request);

    record ProposalRequest(
            int householdSize,
            int dinnerCount,
            Integer maxCookingMinutes,
            Set<String> dietaryRules,
            Set<String> allergens,
            Set<String> excludedIngredientKeys) {
        public ProposalRequest {
            dietaryRules = Set.copyOf(dietaryRules);
            allergens = Set.copyOf(allergens);
            excludedIngredientKeys = Set.copyOf(excludedIngredientKeys);
        }
    }

    record ProposalBatch(String schemaVersion, String source, List<MealProposal> meals) {
        public ProposalBatch {
            meals = List.copyOf(meals);
        }
    }

    record MealProposal(
            String title,
            String description,
            int servings,
            int minutes,
            Set<String> declaredAllergens,
            List<IngredientProposal> ingredients,
            List<String> method) {
        public MealProposal {
            declaredAllergens = Set.copyOf(declaredAllergens);
            ingredients = List.copyOf(ingredients);
            method = List.copyOf(method);
        }
    }

    record IngredientProposal(String canonicalIngredientKey, String displayName, BigDecimal quantity, Unit unit) {}
}
