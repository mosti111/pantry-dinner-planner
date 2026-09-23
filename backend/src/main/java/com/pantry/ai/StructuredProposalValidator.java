package com.pantry.ai;

import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class StructuredProposalValidator {
    public MealProposalGateway.ProposalBatch validate(
            MealProposalGateway.ProposalBatch batch,
            MealProposalGateway.ProposalRequest request) {
        if (!"1.0".equals(batch.schemaVersion())) throw new IllegalArgumentException("Unsupported AI proposal schema");
        if (batch.meals().size() != request.dinnerCount()) {
            throw new IllegalArgumentException("AI proposal meal count does not match the request");
        }
        Set<String> titles = new HashSet<>();
        for (MealProposalGateway.MealProposal meal : batch.meals()) {
            if (meal.title() == null || meal.title().isBlank() || !titles.add(meal.title())) {
                throw new IllegalArgumentException("AI proposal meal titles must be non-empty and unique");
            }
            if (meal.servings() != request.householdSize()) throw new IllegalArgumentException("AI proposal servings mismatch");
            if (meal.minutes() <= 0 || meal.minutes() > 240) throw new IllegalArgumentException("AI proposal cooking time is invalid");
            if (request.maxCookingMinutes() != null && meal.minutes() > request.maxCookingMinutes()) {
                throw new IllegalArgumentException("AI proposal exceeds cooking-time constraint");
            }
            if (meal.ingredients().isEmpty() || meal.method().isEmpty()) {
                throw new IllegalArgumentException("AI proposal recipe is incomplete");
            }
            if (meal.declaredAllergens().stream().anyMatch(request.allergens()::contains)) {
                throw new IllegalArgumentException("AI proposal conflicts with an allergen constraint");
            }
            for (MealProposalGateway.IngredientProposal ingredient : meal.ingredients()) {
                if (ingredient.canonicalIngredientKey() == null || ingredient.canonicalIngredientKey().isBlank()
                        || ingredient.quantity() == null || ingredient.quantity().signum() <= 0 || ingredient.unit() == null) {
                    throw new IllegalArgumentException("AI proposal ingredient is invalid");
                }
                if (request.excludedIngredientKeys().contains(ingredient.canonicalIngredientKey())) {
                    throw new IllegalArgumentException("AI proposal contains an excluded ingredient");
                }
            }
        }
        return batch;
    }
}
