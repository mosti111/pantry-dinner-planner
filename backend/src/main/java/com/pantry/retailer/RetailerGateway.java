package com.pantry.retailer;

import com.pantry.shared.Money;
import com.pantry.shared.Quantity;
import java.util.List;

public interface RetailerGateway {
    List<RetailerOffer> findOffers(String ingredientKey, Quantity.Unit unit);

    RetailerOffer verify(String sku);

    CartResult createCart(String idempotencyKey, List<CartLine> lines);

    RetailerCapabilities capabilities();

    record RetailerOffer(
            String sku,
            String ingredientKey,
            String brand,
            String productName,
            Quantity packageSize,
            Money packagePrice,
            boolean weighted,
            Availability availability,
            String snapshotRevision) {}

    record CartLine(String sku, int packageCount) {}

    record CartResult(String reference, String status, List<String> warnings) {
        public CartResult {
            warnings = List.copyOf(warnings);
        }
    }

    record RetailerCapabilities(
            boolean catalogRead,
            boolean livePrice,
            boolean liveStock,
            boolean cartCreation,
            boolean cartMutation,
            boolean checkoutHandoff) {}

    enum Availability { AVAILABLE, LOW_STOCK, UNAVAILABLE, UNKNOWN }
}
