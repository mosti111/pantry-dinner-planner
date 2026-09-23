package com.pantry.retailer.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.pantry.retailer.RetailerGateway.CartLine;
import com.pantry.shared.Quantity.Unit;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReferenceRetailerGatewayTest {
    private final ReferenceRetailerGateway gateway = new ReferenceRetailerGateway();

    @Test
    void exposesMultipleCanonicalOffersWithoutClaimingLiveCapabilities() {
        var offers = gateway.findOffers("green-lentils", Unit.GRAM);

        assertThat(offers).hasSize(2).allMatch(offer -> offer.ingredientKey().equals("green-lentils"));
        assertThat(gateway.capabilities().catalogRead()).isTrue();
        assertThat(gateway.capabilities().livePrice()).isFalse();
        assertThat(gateway.capabilities().liveStock()).isFalse();
        assertThat(gateway.capabilities().checkoutHandoff()).isFalse();
    }

    @Test
    void usesTheIdempotencyKeyForAStableReferenceCart() {
        var first = gateway.createCart("cart-key", List.of(new CartLine("GREEN-LENTILS", 2)));
        var replay = gateway.createCart("cart-key", List.of(new CartLine("GREEN-LENTILS", 2)));

        assertThat(replay.reference()).isEqualTo(first.reference());
        assertThat(replay.status()).isEqualTo("CREATED");
    }
}
