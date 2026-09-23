package com.pantry.ingredient;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class IngredientNormalizer {
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("mantar", "mushrooms"),
            Map.entry("button-mushroom", "mushrooms"),
            Map.entry("kultur-mantari", "mushrooms"),
            Map.entry("yesil-mercimek", "green-lentils"),
            Map.entry("kirmizi-mercimek", "red-lentils"),
            Map.entry("nohut", "chickpeas"),
            Map.entry("pirinc", "rice"),
            Map.entry("zeytinyagi", "olive-oil"),
            Map.entry("sarmisak", "garlic"),
            Map.entry("kirmizi-biber", "red-pepper"),
            Map.entry("brokoli", "broccoli"),
            Map.entry("makarna", "pasta"));

    public String canonicalize(String rawName) {
        if (rawName == null || rawName.isBlank()) return "";
        String folded = Normalizer.normalize(rawName.trim(), Normalizer.Form.NFKD)
                .replace('ı', 'i')
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return ALIASES.getOrDefault(folded, folded);
    }
}
