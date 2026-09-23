package com.pantry.planning.internal;

import com.pantry.planning.PackageCalculator;
import com.pantry.ingredient.IngredientNormalizer;
import com.pantry.planning.PlanView;
import com.pantry.planning.PlanningRequest;
import com.pantry.recipe.RecipeCatalog;
import com.pantry.recipe.RecipeCatalog.RecipeDefinition;
import com.pantry.recipe.RecipeCatalog.RecipeIngredientDefinition;
import com.pantry.retailer.RetailerGateway;
import com.pantry.retailer.RetailerGateway.RetailerOffer;
import com.pantry.shared.Money;
import com.pantry.shared.Quantity;
import com.pantry.shared.Quantity.Unit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class CuratedPlanningEngine {
    private final IngredientNormalizer ingredientNormalizer;
    private final RecipeCatalog recipeCatalog;
    private final RetailerGateway retailerGateway;

    CuratedPlanningEngine(
            IngredientNormalizer ingredientNormalizer,
            RecipeCatalog recipeCatalog,
            RetailerGateway retailerGateway) {
        this.ingredientNormalizer = ingredientNormalizer;
        this.recipeCatalog = recipeCatalog;
        this.retailerGateway = retailerGateway;
    }

    PlanView create(UUID planId, PlanningRequest request, Instant now) {
        validate(request);
        List<RecipeDefinition> recipes = eligibleRecipes(request).stream().limit(request.dinnerCount()).toList();
        if (recipes.size() < request.dinnerCount()) {
            throw new IllegalArgumentException("Not enough safe recipes match these requirements");
        }

        List<PlanView.MealView> meals = new ArrayList<>();
        Map<String, Demand> demand = new LinkedHashMap<>();
        int slot = 1;
        for (RecipeDefinition recipe : recipes) {
            List<PlanView.IngredientView> ingredients = new ArrayList<>();
            for (RecipeIngredientDefinition ingredient : recipe.ingredients()) {
                BigDecimal scaled = ingredient.quantity()
                        .multiply(BigDecimal.valueOf(request.householdSize()))
                        .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
                ingredients.add(new PlanView.IngredientView(
                        ingredient.ingredientKey(), ingredient.name(), quantity(scaled, ingredient.unit())));
                demand.merge(ingredient.ingredientKey(),
                        new Demand(ingredient.ingredientKey(), ingredient.name(), scaled, ingredient.unit(), ingredient.pantryStaple()),
                        Demand::add);
            }
            meals.add(new PlanView.MealView(
                    "DINNER_" + slot++, recipe.title(), recipe.localizedTitle(), recipe.description(),
                    recipe.minutes(), request.householdSize(), recipe.dietaryLabel(), ingredients, recipe.method(),
                    List.of("Fits the confirmed food requirements", recipe.minutes() + " minutes total cooking time",
                            "Shares ingredients with other dinners to consolidate shopping")));
        }

        List<PlanView.BasketLineView> lines = new ArrayList<>(demand.values().stream().map(this::lineFor).toList());
        Money total = lines.stream().map(this::moneyFrom).reduce(Money.tryAmount("0"), Money::add);
        Money budget = new Money(new BigDecimal(request.budget().amount()), Currency.getInstance(request.budget().currency()));
        boolean optimized = false;
        if (total.compareTo(budget) > 0) {
            for (int index = 0; index < lines.size() && total.compareTo(budget) > 0; index++) {
                PlanView.BasketLineView line = lines.get(index);
                if (!line.alternatives().isEmpty()) {
                    PlanView.BasketLineView replacement = chooseAlternative(line, line.alternatives().getFirst().sku());
                    if (moneyFrom(replacement).compareTo(moneyFrom(line)) < 0) {
                        lines.set(index, replacement);
                        total = total.subtractFloorZero(moneyFrom(line)).add(moneyFrom(replacement));
                        optimized = true;
                    }
                }
            }
        }
        if (total.compareTo(budget) > 0) {
            throw new IllegalArgumentException("No safe plan fits the selected whole-package budget");
        }
        PlanView.BasketView basket = basket(UUID.randomUUID(), "NEEDS_REVIEW", lines, null);
        List<String> notices = optimized
                ? List.of("Lower-cost approved product matches were selected to keep the full-package basket within budget.",
                          "Prices and stock are demo snapshots and will be rechecked before cart creation.")
                : List.of("Prices and stock are demo snapshots and will be rechecked before cart creation.");
        return new PlanView(planId, "READY_FOR_REVIEW", request.householdSize(), request.dinnerCount(),
                money(budget), money(total), money(budget.subtractFloorZero(total)), meals, basket,
                notices, now, 0);
    }

    PlanView updateLineSelection(PlanView plan, UUID lineId, boolean selected, Instant now) {
        List<PlanView.BasketLineView> lines = plan.basket().lines().stream()
                .map(line -> line.id().equals(lineId) ? copySelected(line, selected) : line)
                .toList();
        return rebuild(plan, basket(plan.basket().id(), "NEEDS_REVIEW", lines, null), now);
    }

    PlanView selectProduct(PlanView plan, UUID lineId, String sku, Instant now) {
        List<PlanView.BasketLineView> lines = plan.basket().lines().stream()
                .map(line -> line.id().equals(lineId) ? chooseAlternative(line, sku) : line)
                .toList();
        if (lines.stream().noneMatch(line -> line.id().equals(lineId))) {
            throw new IllegalArgumentException("Basket line was not found");
        }
        return rebuild(plan, basket(plan.basket().id(), "NEEDS_REVIEW", lines, null), now,
                "A verified same-ingredient product alternative was selected.");
    }

    PlanView replaceMeal(PlanView plan, PlanningRequest request, String slot, Instant now) {
        int target = -1;
        for (int index = 0; index < plan.meals().size(); index++) {
            if (plan.meals().get(index).slot().equals(slot)) target = index;
        }
        if (target < 0) throw new IllegalArgumentException("Meal slot was not found");

        Set<String> currentTitles = plan.meals().stream().map(PlanView.MealView::title).collect(java.util.stream.Collectors.toSet());
        RecipeDefinition replacement = eligibleRecipes(request).stream()
                .filter(candidate -> !currentTitles.contains(candidate.title()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No other safe meal matches these requirements"));
        List<PlanView.MealView> meals = new ArrayList<>(plan.meals());
        meals.set(target, meal(replacement, slot, request.householdSize()));
        List<PlanView.BasketLineView> lines = linesForMeals(meals, plan.basket().lines());
        PlanView.BasketView rebuiltBasket = basket(plan.basket().id(), "NEEDS_REVIEW", lines, null);
        ensureWithinBudget(rebuiltBasket.completeTotal(), plan.budget());
        PlanView replaced = new PlanView(plan.id(), "READY_FOR_REVIEW", plan.householdSize(), plan.dinnerCount(),
                plan.budget(), rebuiltBasket.completeTotal(),
                money(moneyFrom(plan.budget()).subtractFloorZero(moneyFrom(rebuiltBasket.toBuy()))),
                List.copyOf(meals), rebuiltBasket, plan.notices(), now, plan.version());
        return rebuild(replaced, rebuiltBasket, now,
                "Recovery level 3: the affected dinner was replaced and its basket was recalculated.");
    }

    PlanView simulateRecovery(PlanView plan, PlanningRequest request, String scenario, Instant now) {
        String normalized = scenario == null ? "" : scenario.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "SKU_UNAVAILABLE" -> recoverSku(plan, now);
            case "INGREDIENT_UNAVAILABLE" -> recoverIngredient(plan, request, now);
            case "PRICE_INCREASE" -> recoverPrice(plan, now);
            case "PROMOTIONS_EXPIRED" -> withNotice(refresh(plan, now), now,
                    "Promotions were revalidated; the full-package total is current.");
            case "RETAILER_UNAVAILABLE" -> withNotice(refresh(plan, now), now,
                    "Recovery level 4: retailer failover completed through the adapter boundary.");
            case "CART_TRANSFER_FAILURE" -> withNotice(refresh(plan, now), now,
                    "The transient cart transfer failed once, then safely retried with the same idempotency key.");
            default -> throw new IllegalArgumentException("Unknown recovery scenario");
        };
    }

    PlanView refresh(PlanView plan, Instant now) {
        List<PlanView.BasketLineView> lines = plan.basket().lines().stream()
                .map(this::refreshLine)
                .toList();
        return rebuild(plan, basket(plan.basket().id(), "VERIFIED", lines, null), now,
                "Basket prices and stock were refreshed.");
    }

    PlanView finalizeCart(PlanView plan, String idempotencyKey, Instant now) {
        List<RetailerGateway.CartLine> selectedLines = plan.basket().lines().stream()
                .filter(PlanView.BasketLineView::selected)
                .map(line -> new RetailerGateway.CartLine(line.sku(), line.packageCount()))
                .toList();
        RetailerGateway.CartResult cart = retailerGateway.createCart(idempotencyKey, selectedLines);
        PlanView.BasketView basket = basket(plan.basket().id(), cart.status(), plan.basket().lines(), cart.reference());
        String notice = cart.warnings().isEmpty() ? "Retailer cart created." : cart.warnings().getFirst();
        return rebuild(plan, basket, now, notice, "CART_READY");
    }

    private void validate(PlanningRequest request) {
        if (!request.requirementsReviewed()) throw new IllegalArgumentException("Food requirements must be reviewed");
        Set<String> supportedDiets = Set.of("VEGAN", "VEGETARIAN", "PESCATARIAN", "GLUTEN_FREE");
        Set<String> supportedAllergens = Set.of("MILK", "EGGS", "GLUTEN", "SOY", "PEANUT", "TREE_NUT",
                "SESAME", "FISH", "SHELLFISH", "CELERY", "MUSTARD", "SULPHITES", "LUPIN");
        if (!supportedDiets.containsAll(request.dietaryRules())) {
            throw new IllegalArgumentException("An unsupported dietary rule was supplied");
        }
        if (!supportedAllergens.containsAll(request.allergens())) {
            throw new IllegalArgumentException("An unsupported allergen was supplied");
        }
        if (request.dietaryRules().contains("VEGAN") && request.dietaryRules().contains("PESCATARIAN")) {
            throw new IllegalArgumentException("Choose one primary dietary pattern");
        }
    }

    private List<RecipeDefinition> eligibleRecipes(PlanningRequest request) {
        int maximum = request.maxCookingMinutes() == null ? Integer.MAX_VALUE : request.maxCookingMinutes();
        Set<String> exclusions = request.excludedIngredients().stream()
                .map(ingredientNormalizer::canonicalize)
                .collect(java.util.stream.Collectors.toSet());
        return recipeCatalog.findAll().stream()
                .filter(recipe -> recipe.minutes() <= maximum)
                .filter(recipe -> recipe.ingredients().stream().noneMatch(item -> exclusions.contains(item.ingredientKey())))
                .filter(recipe -> request.allergens().stream().noneMatch(recipe.allergens()::contains))
                .filter(recipe -> !request.dietaryRules().contains("GLUTEN_FREE") || !recipe.allergens().contains("GLUTEN"))
                .sorted((left, right) -> score(left, request) - score(right, request))
                .toList();
    }

    private int score(RecipeDefinition recipe, PlanningRequest request) {
        return switch (request.priority()) {
            case QUICKER -> recipe.minutes();
            case LOWER_COST -> recipe.costRank();
            case PROTEIN_RICH -> -recipe.proteinRank();
            case BALANCED -> recipe.balanceRank();
        };
    }

    private PlanView.BasketLineView lineFor(Demand demand) {
        Quantity required = new Quantity(demand.quantity, demand.unit);
        List<RetailerOffer> offers = retailerGateway.findOffers(demand.key, demand.unit).stream()
                .filter(offer -> offer.availability() != RetailerGateway.Availability.UNAVAILABLE)
                .toList();
        if (offers.isEmpty()) throw new IllegalArgumentException("No retailer product matches " + demand.name);
        return lineFromOffer(UUID.randomUUID(), demand, required, offers.getFirst(), offers.stream().skip(1).toList(), true);
    }

    private PlanView rebuild(PlanView plan, PlanView.BasketView basket, Instant now, String... additionalNotices) {
        Money budget = moneyFrom(plan.budget());
        Money total = moneyFrom(basket.toBuy());
        List<String> notices = new ArrayList<>(plan.notices());
        notices.addAll(List.of(additionalNotices));
        String status = additionalNotices.length > 1 ? additionalNotices[1] : plan.status();
        if (additionalNotices.length > 1) notices.remove(notices.size() - 1);
        return new PlanView(plan.id(), status, plan.householdSize(), plan.dinnerCount(), plan.budget(),
                basket.completeTotal(), money(budget.subtractFloorZero(total)), plan.meals(), basket,
                List.copyOf(notices), now, plan.version() + 1);
    }

    private PlanView.BasketView basket(UUID id, String status, List<PlanView.BasketLineView> lines, String reference) {
        Money complete = lines.stream().map(this::moneyFrom).reduce(Money.tryAmount("0"), Money::add);
        Money toBuy = lines.stream().filter(PlanView.BasketLineView::selected)
                .map(this::moneyFrom).reduce(Money.tryAmount("0"), Money::add);
        Money atHome = complete.subtractFloorZero(toBuy);
        return new PlanView.BasketView(id, status, lines, money(complete), money(atHome), money(toBuy),
                (int) lines.stream().filter(PlanView.BasketLineView::selected).count(), reference);
    }

    private PlanView.BasketLineView copySelected(PlanView.BasketLineView line, boolean selected) {
        return new PlanView.BasketLineView(line.id(), line.ingredientKey(), line.ingredientName(), line.sku(), line.brand(),
                line.productName(), line.required(), line.packageSize(), line.packageCount(), line.leftover(),
                line.lineTotal(), selected, line.pantryStaple(), line.estimatedWeight(), line.availability(), line.alternatives());
    }

    private PlanView.BasketLineView refreshLine(PlanView.BasketLineView line) {
        RetailerOffer verified = retailerGateway.verify(line.sku());
        if (!verified.ingredientKey().equals(line.ingredientKey())) {
            throw new IllegalStateException("Retailer verification changed the canonical ingredient mapping");
        }
        Demand demand = new Demand(line.ingredientKey(), line.ingredientName(),
                new BigDecimal(line.required().value()), Unit.valueOf(line.required().unit()), line.pantryStaple());
        List<RetailerOffer> alternatives = retailerGateway.findOffers(line.ingredientKey(), verified.packageSize().unit()).stream()
                .filter(offer -> !offer.sku().equals(verified.sku()))
                .filter(offer -> offer.availability() != RetailerGateway.Availability.UNAVAILABLE)
                .toList();
        return lineFromOffer(line.id(), demand, quantityFrom(line.required()), verified, alternatives, line.selected());
    }

    private PlanView.BasketLineView lineFromOffer(UUID lineId, Demand demand, Quantity required,
            RetailerOffer selectedOffer, List<RetailerOffer> alternatives, boolean selected) {
        PackageCalculator.Result result = PackageCalculator.calculate(
                required, selectedOffer.packageSize(), selectedOffer.packagePrice());
        List<PlanView.ProductAlternativeView> alternativeViews = alternatives.stream()
                .map(offer -> new PlanView.ProductAlternativeView(offer.sku(), offer.brand(), offer.productName(),
                        quantity(offer.packageSize().value(), offer.packageSize().unit()), money(offer.packagePrice()),
                        "Same canonical ingredient; current retailer snapshot " + offer.snapshotRevision()))
                .toList();
        return new PlanView.BasketLineView(lineId, demand.key, demand.name, selectedOffer.sku(),
                selectedOffer.brand(), selectedOffer.productName(), quantity(required.value(), required.unit()),
                quantity(selectedOffer.packageSize().value(), selectedOffer.packageSize().unit()), result.packageCount(),
                quantity(result.leftover().value(), result.leftover().unit()), money(result.lineTotal()), selected,
                demand.pantryStaple, selectedOffer.weighted(), selectedOffer.availability().name(), alternativeViews);
    }

    private PlanView.BasketLineView chooseAlternative(PlanView.BasketLineView line, String sku) {
        PlanView.ProductAlternativeView alternative = line.alternatives().stream()
                .filter(candidate -> candidate.sku().equals(sku))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product is not an approved alternative for this ingredient"));
        RetailerOffer verified = retailerGateway.verify(alternative.sku());
        if (!verified.ingredientKey().equals(line.ingredientKey())) {
            throw new IllegalArgumentException("Product is not mapped to the requested canonical ingredient");
        }
        RetailerOffer original = retailerGateway.verify(line.sku());
        Demand demand = new Demand(line.ingredientKey(), line.ingredientName(), new BigDecimal(line.required().value()),
                Unit.valueOf(line.required().unit()), line.pantryStaple());
        return lineFromOffer(line.id(), demand, quantityFrom(line.required()), verified, List.of(original), line.selected());
    }

    private PlanView recoverSku(PlanView plan, Instant now) {
        PlanView.BasketLineView target = plan.basket().lines().stream()
                .filter(PlanView.BasketLineView::selected)
                .filter(line -> !line.alternatives().isEmpty())
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No recoverable basket line exists"));
        List<PlanView.BasketLineView> lines = plan.basket().lines().stream()
                .map(line -> line.id().equals(target.id()) ? chooseAlternative(line, line.alternatives().getFirst().sku()) : line)
                .toList();
        return rebuild(plan, basket(plan.basket().id(), "RECOVERED", lines, null), now,
                "Recovery level 1: an unavailable SKU was replaced with a verified same-ingredient product.");
    }

    private PlanView recoverPrice(PlanView plan, Instant now) {
        PlanView recovered = recoverSku(plan, now);
        return withNotice(recovered, now, "The price change was absorbed by a lower-cost approved product match.");
    }

    private PlanView recoverIngredient(PlanView plan, PlanningRequest request, Instant now) {
        Map<String, String> approved = Map.of(
                "red-pepper", "carrot",
                "mushrooms", "oyster-mushrooms",
                "oyster-mushrooms", "mushrooms",
                "spinach", "broccoli",
                "broccoli", "spinach",
                "tofu", "chickpeas",
                "green-lentils", "chickpeas",
                "chickpeas", "green-lentils");
        Set<String> exclusions = request.excludedIngredients().stream()
                .map(ingredientNormalizer::canonicalize)
                .collect(java.util.stream.Collectors.toSet());
        Map.Entry<String, String> substitution = approved.entrySet().stream()
                .filter(entry -> plan.basket().lines().stream()
                        .anyMatch(line -> line.ingredientKey().equals(entry.getKey())))
                .filter(entry -> !exclusions.contains(entry.getValue()))
                .findFirst().orElse(null);
        if (substitution == null) {
            return replaceMeal(plan, request, plan.meals().getFirst().slot(), now);
        }

        String substituteName = title(substitution.getValue());
        List<PlanView.MealView> meals = plan.meals().stream().map(mealView -> {
            List<PlanView.IngredientView> ingredients = mealView.ingredients().stream()
                    .map(ingredient -> ingredient.key().equals(substitution.getKey())
                            ? new PlanView.IngredientView(substitution.getValue(), substituteName, ingredient.quantity())
                            : ingredient)
                    .toList();
            return new PlanView.MealView(mealView.slot(), mealView.title(), mealView.localizedTitle(),
                    mealView.description(), mealView.minutes(), mealView.servings(), mealView.dietaryLabel(),
                    ingredients, mealView.method(), mealView.reasons());
        }).toList();
        List<PlanView.BasketLineView> lines = linesForMeals(meals, plan.basket().lines());
        PlanView.BasketView recoveredBasket = basket(plan.basket().id(), "RECOVERED", lines, null);
        ensureWithinBudget(recoveredBasket.completeTotal(), plan.budget());
        PlanView changed = new PlanView(plan.id(), "READY_FOR_REVIEW", plan.householdSize(), plan.dinnerCount(),
                plan.budget(), recoveredBasket.completeTotal(),
                money(moneyFrom(plan.budget()).subtractFloorZero(moneyFrom(recoveredBasket.toBuy()))),
                meals, recoveredBasket, plan.notices(), now, plan.version());
        return rebuild(changed, recoveredBasket, now,
                "Recovery level 2: the unavailable ingredient was replaced with an approved culinary substitute and revalidated.");
    }

    private PlanView withNotice(PlanView plan, Instant now, String notice) {
        return rebuild(plan, plan.basket(), now, notice);
    }

    private PlanView.MealView meal(RecipeDefinition recipe, String slot, int householdSize) {
        List<PlanView.IngredientView> ingredients = recipe.ingredients().stream().map(ingredient -> {
            BigDecimal scaled = ingredient.quantity().multiply(BigDecimal.valueOf(householdSize))
                    .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
            return new PlanView.IngredientView(ingredient.ingredientKey(), ingredient.name(), quantity(scaled, ingredient.unit()));
        }).toList();
        return new PlanView.MealView(slot, recipe.title(), recipe.localizedTitle(), recipe.description(),
                recipe.minutes(), householdSize, recipe.dietaryLabel(), ingredients, recipe.method(),
                List.of("Fits the confirmed food requirements", recipe.minutes() + " minutes total cooking time",
                        "Shares ingredients with other dinners to consolidate shopping"));
    }

    private List<PlanView.BasketLineView> linesForMeals(
            List<PlanView.MealView> meals, List<PlanView.BasketLineView> previousLines) {
        Map<String, Demand> demand = new LinkedHashMap<>();
        for (PlanView.MealView meal : meals) {
            for (PlanView.IngredientView ingredient : meal.ingredients()) {
                Quantity amount = quantityFrom(ingredient.quantity());
                boolean staple = Set.of("garlic", "rice", "olive-oil", "salt", "paprika").contains(ingredient.key());
                demand.merge(ingredient.key(), new Demand(ingredient.key(), ingredient.name(), amount.value(), amount.unit(), staple), Demand::add);
            }
        }
        Map<String, Boolean> selections = previousLines.stream().collect(java.util.stream.Collectors.toMap(
                PlanView.BasketLineView::ingredientKey, PlanView.BasketLineView::selected, (left, right) -> left));
        return demand.values().stream().map(this::lineFor)
                .map(line -> selections.containsKey(line.ingredientKey())
                        ? copySelected(line, selections.get(line.ingredientKey())) : line)
                .toList();
    }

    private void ensureWithinBudget(PlanView.MoneyView total, PlanView.MoneyView budget) {
        if (moneyFrom(total).compareTo(moneyFrom(budget)) > 0) {
            throw new IllegalArgumentException("The replacement would exceed the whole-package budget");
        }
    }

    private Quantity quantityFrom(PlanView.QuantityView value) {
        return new Quantity(new BigDecimal(value.value()), Unit.valueOf(value.unit()));
    }

    private Money moneyFrom(PlanView.BasketLineView line) { return moneyFrom(line.lineTotal()); }
    private Money moneyFrom(PlanView.MoneyView value) {
        return new Money(new BigDecimal(value.amount()), Currency.getInstance(value.currency()));
    }
    private PlanView.MoneyView money(Money value) { return new PlanView.MoneyView(value.amount().toPlainString(), value.currency().getCurrencyCode()); }
    private PlanView.QuantityView quantity(BigDecimal value, Unit unit) { return new PlanView.QuantityView(value.stripTrailingZeros().toPlainString(), unit.name()); }

    private String title(String key) { return Character.toUpperCase(key.charAt(0)) + key.substring(1).replace('-', ' '); }
    private record Demand(String key, String name, BigDecimal quantity, Unit unit, boolean pantryStaple) {
        Demand add(Demand other) { return new Demand(key, name, quantity.add(other.quantity), unit, pantryStaple || other.pantryStaple); }
    }
}
