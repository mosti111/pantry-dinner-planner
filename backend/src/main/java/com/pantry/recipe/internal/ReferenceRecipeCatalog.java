package com.pantry.recipe.internal;

import com.pantry.recipe.RecipeCatalog;
import com.pantry.recipe.RecipeCatalog.RecipeDefinition;
import com.pantry.recipe.RecipeCatalog.RecipeIngredientDefinition;
import com.pantry.shared.Quantity.Unit;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ReferenceRecipeCatalog implements RecipeCatalog {
    private final List<RecipeDefinition> recipes = List.of(
            recipe("lentil-mushroom-rice", "Lentils and mushrooms with rice", "Mantarlı mercimek ve pirinç pilavı",
                    "Green lentils, mushrooms and spinach served with rice.", 35, 1, 3, 1, Set.of(),
                    List.of(i("green-lentils", "Cooked green lentils", "800", Unit.GRAM, false), i("mushrooms", "Button mushrooms", "350", Unit.GRAM, false), i("spinach", "Spinach", "250", Unit.GRAM, false), i("onion", "Onion", "150", Unit.GRAM, false), i("garlic", "Garlic", "8", Unit.GRAM, true), i("rice", "Long-grain rice", "280", Unit.GRAM, true), i("olive-oil", "Olive oil", "30", Unit.MILLILITER, true), i("salt", "Salt", "4", Unit.GRAM, true), i("paprika", "Sweet paprika", "3", Unit.GRAM, true)),
                    List.of("Cook the rice according to its package ratio.", "Soften onion and garlic, then cook the lentils and vegetables.", "Season and divide evenly.")),
            recipe("oyster-mushroom-chickpea-rice", "Oyster mushroom chickpeas with rice", "İstiridye mantarlı nohut ve pirinç pilavı",
                    "Chickpeas, oyster mushrooms and red pepper served with rice.", 40, 2, 4, 2, Set.of(),
                    List.of(i("chickpeas", "Cooked chickpeas", "800", Unit.GRAM, false), i("oyster-mushrooms", "Oyster mushrooms", "350", Unit.GRAM, false), i("red-pepper", "Red pepper", "250", Unit.GRAM, false), i("onion", "Onion", "150", Unit.GRAM, false), i("garlic", "Garlic", "8", Unit.GRAM, true), i("rice", "Long-grain rice", "280", Unit.GRAM, true), i("olive-oil", "Olive oil", "30", Unit.MILLILITER, true), i("salt", "Salt", "4", Unit.GRAM, true), i("paprika", "Sweet paprika", "3", Unit.GRAM, true)),
                    List.of("Cook the rice.", "Sauté the mushrooms, pepper and aromatics.", "Fold in chickpeas, season and serve.")),
            recipe("paprika-tofu-rice", "Paprika tofu with rice", "Biberli tofu ve pirinç pilavı",
                    "Firm tofu, broccoli and red pepper served with rice.", 35, 3, 5, 3, Set.of("SOY"),
                    List.of(i("tofu", "Plain firm tofu", "650", Unit.GRAM, false), i("broccoli", "Broccoli", "350", Unit.GRAM, false), i("red-pepper", "Red pepper", "250", Unit.GRAM, false), i("rice", "Long-grain rice", "280", Unit.GRAM, true), i("olive-oil", "Olive oil", "30", Unit.MILLILITER, true), i("salt", "Salt", "4", Unit.GRAM, true), i("paprika", "Sweet paprika", "3", Unit.GRAM, true)),
                    List.of("Cook the rice.", "Brown tofu, then add broccoli and pepper.", "Season with paprika and serve.")),
            recipe("red-lentil-bulgur-bowl", "Red lentil vegetable bowls", "Sebzeli kırmızı mercimek kasesi",
                    "Red lentils with seasonal vegetables and bulgur.", 30, 0, 4, 4, Set.of("GLUTEN"),
                    List.of(i("red-lentils", "Dry red lentils", "320", Unit.GRAM, false), i("carrot", "Carrot", "300", Unit.GRAM, false), i("bulgur", "Coarse bulgur", "280", Unit.GRAM, false), i("onion", "Onion", "150", Unit.GRAM, false), i("olive-oil", "Olive oil", "30", Unit.MILLILITER, true), i("salt", "Salt", "4", Unit.GRAM, true)),
                    List.of("Simmer the lentils.", "Cook bulgur separately.", "Sauté vegetables and combine.")),
            recipe("white-bean-tomato-pasta", "White bean tomato pasta", "Fasulyeli domatesli makarna",
                    "White beans and tomato sauce with wholewheat pasta.", 30, 1, 4, 5, Set.of("GLUTEN"),
                    List.of(i("white-beans", "Cooked white beans", "700", Unit.GRAM, false), i("pasta", "Wholewheat pasta", "400", Unit.GRAM, false), i("tomato-passata", "Tomato passata", "500", Unit.MILLILITER, false), i("onion", "Onion", "150", Unit.GRAM, false), i("olive-oil", "Olive oil", "30", Unit.MILLILITER, true), i("salt", "Salt", "4", Unit.GRAM, true)),
                    List.of("Boil the pasta.", "Simmer tomato, onion and beans.", "Combine and serve.")),
            recipe("potato-pea-stew", "Potato and pea tomato stew", "Patatesli bezelye yemeği",
                    "Potato, peas and tomato served with fragrant rice.", 30, 1, 3, 6, Set.of(),
                    List.of(i("potatoes", "Potatoes", "700", Unit.GRAM, false), i("peas", "Green peas", "350", Unit.GRAM, false), i("tomato-passata", "Tomato passata", "400", Unit.MILLILITER, false), i("onion", "Onion", "150", Unit.GRAM, false), i("rice", "Long-grain rice", "280", Unit.GRAM, true), i("olive-oil", "Olive oil", "30", Unit.MILLILITER, true), i("salt", "Salt", "4", Unit.GRAM, true)),
                    List.of("Cook the rice.", "Simmer potato, peas, tomato and onion until tender.", "Season and serve in bowls.")),
            recipe("white-bean-spinach-rice", "White bean spinach rice bowls", "Ispanaklı kuru fasulye kasesi",
                    "White beans and spinach with tomato rice.", 25, 2, 4, 7, Set.of(),
                    List.of(i("white-beans", "Cooked white beans", "700", Unit.GRAM, false), i("spinach", "Spinach", "300", Unit.GRAM, false), i("tomato-passata", "Tomato passata", "350", Unit.MILLILITER, false), i("rice", "Long-grain rice", "280", Unit.GRAM, true), i("onion", "Onion", "150", Unit.GRAM, false), i("olive-oil", "Olive oil", "30", Unit.MILLILITER, true), i("salt", "Salt", "4", Unit.GRAM, true)),
                    List.of("Cook the tomato rice.", "Soften onion and spinach.", "Fold in beans, heat through and serve.")));

    @Override
    public List<RecipeDefinition> findAll() {
        return recipes;
    }

    private static RecipeDefinition recipe(String key, String title, String localizedTitle, String description,
            int minutes, int costRank, int proteinRank, int balanceRank, Set<String> allergens,
            List<RecipeIngredientDefinition> ingredients, List<String> method) {
        return new RecipeDefinition(key, title, localizedTitle, description, minutes, "Plant-based",
                costRank, proteinRank, balanceRank, allergens, ingredients, method);
    }

    private static RecipeIngredientDefinition i(String key, String name, String quantity, Unit unit, boolean staple) {
        return new RecipeIngredientDefinition(key, name, new BigDecimal(quantity), unit, staple);
    }
}
