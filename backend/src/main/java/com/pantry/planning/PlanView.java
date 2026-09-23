package com.pantry.planning;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlanView(
        UUID id,
        String status,
        int householdSize,
        int dinnerCount,
        MoneyView budget,
        MoneyView total,
        MoneyView remaining,
        List<MealView> meals,
        BasketView basket,
        List<String> notices,
        Instant updatedAt,
        long version) {

    public record MoneyView(String amount, String currency) {}

    public record QuantityView(String value, String unit) {}

    public record MealView(
            String slot,
            String title,
            String localizedTitle,
            String description,
            int minutes,
            int servings,
            String dietaryLabel,
            List<IngredientView> ingredients,
            List<String> method,
            List<String> reasons) {}

    public record IngredientView(String key, String name, QuantityView quantity) {}

    public record BasketView(
            UUID id,
            String status,
            List<BasketLineView> lines,
            MoneyView completeTotal,
            MoneyView alreadyAtHome,
            MoneyView toBuy,
            int selectedCount,
            String cartReference) {}

    public record BasketLineView(
            UUID id,
            String ingredientKey,
            String ingredientName,
            String sku,
            String brand,
            String productName,
            QuantityView required,
            QuantityView packageSize,
            int packageCount,
            QuantityView leftover,
            MoneyView lineTotal,
            boolean selected,
            boolean pantryStaple,
            boolean estimatedWeight,
            String availability,
            List<ProductAlternativeView> alternatives) {}

    public record ProductAlternativeView(
            String sku,
            String brand,
            String productName,
            QuantityView packageSize,
            MoneyView packagePrice,
            String reason) {}
}
