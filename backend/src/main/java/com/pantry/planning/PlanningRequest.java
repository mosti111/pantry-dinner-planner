package com.pantry.planning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.util.Set;

public record PlanningRequest(
        @Min(1) @Max(12) int householdSize,
        @Min(1) @Max(7) int dinnerCount,
        @NotNull @Valid MoneyInput budget,
        @Min(10) @Max(240) Integer maxCookingMinutes,
        @NotNull Preference priority,
        @NotNull @Size(max = 4) Set<@Size(max = 40) String> dietaryRules,
        @NotNull @Size(max = 14) Set<@Size(max = 40) String> allergens,
        @NotNull @Size(max = 30) Set<@Size(max = 100) String> excludedIngredients,
        boolean requirementsReviewed) {

    public PlanningRequest {
        dietaryRules = dietaryRules == null ? Set.of() : Set.copyOf(dietaryRules);
        allergens = allergens == null ? Set.of() : Set.copyOf(allergens);
        excludedIngredients = excludedIngredients == null ? Set.of() : Set.copyOf(excludedIngredients);
    }

    public enum Preference { BALANCED, LOWER_COST, QUICKER, PROTEIN_RICH }

    public record MoneyInput(
            @NotBlank @DecimalMin("1.00") @DecimalMax("100000.00") String amount,
            @NotBlank @Pattern(regexp = "TRY") String currency) {}
}
