package com.pantry.recipe;

import com.pantry.shared.Quantity.Unit;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface RecipeCatalog {
    List<RecipeDefinition> findAll();

    record RecipeDefinition(
            String key,
            String title,
            String localizedTitle,
            String description,
            int minutes,
            String dietaryLabel,
            int costRank,
            int proteinRank,
            int balanceRank,
            Set<String> allergens,
            List<RecipeIngredientDefinition> ingredients,
            List<String> method) {
        public RecipeDefinition {
            allergens = Set.copyOf(allergens);
            ingredients = List.copyOf(ingredients);
            method = List.copyOf(method);
        }
    }

    record RecipeIngredientDefinition(
            String ingredientKey,
            String name,
            BigDecimal quantity,
            Unit unit,
            boolean pantryStaple) {}
}
