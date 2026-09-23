package com.pantry.ingredient;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class IngredientNormalizerTest {
    private final IngredientNormalizer normalizer = new IngredientNormalizer();

    @ParameterizedTest
    @CsvSource({
            "Kültür Mantarı,mushrooms",
            "yeşil mercimek,green-lentils",
            "PİRİNÇ,rice",
            "zeytinyağı,olive-oil",
            "Red pepper,red-pepper"
    })
    void resolvesNamingAndLanguageVariants(String input, String expected) {
        assertThat(normalizer.canonicalize(input)).isEqualTo(expected);
    }
}
