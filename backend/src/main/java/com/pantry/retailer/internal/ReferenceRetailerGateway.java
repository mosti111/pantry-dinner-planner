package com.pantry.retailer.internal;

import com.pantry.retailer.RetailerGateway;
import com.pantry.shared.Money;
import com.pantry.shared.Quantity;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReferenceRetailerGateway implements RetailerGateway {
    private static final Currency TRY = Currency.getInstance("TRY");
    private final Map<String, List<RetailerOffer>> offers;

    public ReferenceRetailerGateway() {
        Map<String, List<RetailerOffer>> catalog = new LinkedHashMap<>();
        add(catalog, "green-lentils", "500", "33.25", "Anadolu Table", false);
        add(catalog, "mushrooms", "350", "15.58", "Daily Harvest", true);
        add(catalog, "spinach", "250", "11.13", "Daily Harvest", true);
        add(catalog, "onion", "450", "20.03", "Daily Harvest", true);
        add(catalog, "garlic", "100", "4.45", "Daily Harvest", true);
        add(catalog, "rice", "1000", "80.10", "Daily Harvest", false);
        add(catalog, "olive-oil", "250", "37.50", "Pantry Market", false);
        add(catalog, "salt", "50", "4.50", "Pantry Market", false);
        add(catalog, "paprika", "50", "4.50", "Pantry Market", false);
        add(catalog, "chickpeas", "500", "33.25", "Anadolu Table", false);
        add(catalog, "oyster-mushrooms", "350", "15.58", "Daily Harvest", true);
        add(catalog, "red-pepper", "500", "22.25", "Daily Harvest", true);
        add(catalog, "tofu", "500", "42.75", "Anadolu Table", false);
        add(catalog, "broccoli", "350", "15.58", "Daily Harvest", true);
        add(catalog, "red-lentils", "500", "30.00", "Anadolu Table", false);
        add(catalog, "carrot", "500", "18.00", "Daily Harvest", true);
        add(catalog, "bulgur", "1000", "55.00", "Anadolu Table", false);
        add(catalog, "white-beans", "500", "34.00", "Anadolu Table", false);
        add(catalog, "pasta", "500", "36.00", "Pantry Market", false);
        add(catalog, "tomato-passata", "500", "28.00", "Pantry Market", false);
        add(catalog, "potatoes", "1000", "32.00", "Daily Harvest", true);
        add(catalog, "peas", "450", "27.00", "Daily Harvest", false);
        offers = Map.copyOf(catalog);
    }

    @Override
    public List<RetailerOffer> findOffers(String ingredientKey, Quantity.Unit unit) {
        return offers.getOrDefault(ingredientKey, List.of()).stream()
                .filter(offer -> offer.packageSize().unit() == unit)
                .toList();
    }

    @Override
    public RetailerOffer verify(String sku) {
        return offers.values().stream().flatMap(List::stream)
                .filter(offer -> offer.sku().equals(sku))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Retailer SKU is unknown"));
    }

    @Override
    public CartResult createCart(String idempotencyKey, List<CartLine> lines) {
        if (!capabilities().cartCreation()) throw new IllegalStateException("Retailer does not support cart creation");
        if (lines.isEmpty()) throw new IllegalArgumentException("A cart needs at least one selected line");
        String reference = "DEMO-" + digest(idempotencyKey + lines).substring(0, 10).toUpperCase();
        return new CartResult(reference, "CREATED", List.of("Reference retailer only; no order or payment was placed."));
    }

    @Override
    public RetailerCapabilities capabilities() {
        return new RetailerCapabilities(true, false, false, true, false, false);
    }

    private void add(Map<String, List<RetailerOffer>> target, String ingredientKey, String size,
            String price, String brand, boolean weighted) {
        Quantity.Unit unit = ingredientKey.equals("tomato-passata") || ingredientKey.equals("olive-oil")
                ? Quantity.Unit.MILLILITER : Quantity.Unit.GRAM;
        BigDecimal packageSize = new BigDecimal(size);
        Money primaryPrice = new Money(new BigDecimal(price), TRY);
        String title = Character.toUpperCase(ingredientKey.charAt(0)) + ingredientKey.substring(1).replace('-', ' ');
        List<RetailerOffer> ingredientOffers = new ArrayList<>();
        ingredientOffers.add(new RetailerOffer(ingredientKey.toUpperCase(), ingredientKey, brand,
                title + (weighted ? " — by weight" : " — " + size + (unit == Quantity.Unit.GRAM ? " g" : " ml")),
                new Quantity(packageSize, unit), primaryPrice, weighted, Availability.AVAILABLE, "reference-v1"));
        ingredientOffers.add(new RetailerOffer(ingredientKey.toUpperCase() + "-ALT", ingredientKey, "Pantry Market",
                title + " alternative", new Quantity(packageSize, unit),
                new Money(primaryPrice.amount().multiply(new BigDecimal("0.94")), TRY), weighted,
                Availability.AVAILABLE, "reference-v1"));
        target.put(ingredientKey, List.copyOf(ingredientOffers));
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
