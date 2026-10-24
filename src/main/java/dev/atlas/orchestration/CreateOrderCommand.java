package dev.atlas.orchestration;

import java.util.UUID;

public record CreateOrderCommand(
        UUID orderId,
        String sku,
        int quantity,
        long amountInMinorUnits,
        String currency
) {
}
