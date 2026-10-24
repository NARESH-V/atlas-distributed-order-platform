package dev.atlas.orchestration;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface OrderActivities {
    @ActivityMethod
    void reserveInventory(String sku, int quantity, String idempotencyKey);

    @ActivityMethod
    void authorizePayment(String orderId, long amountInMinorUnits, String currency, String idempotencyKey);

    @ActivityMethod
    void requestShipment(String orderId, String idempotencyKey);

    @ActivityMethod
    void releaseInventory(String sku, int quantity, String idempotencyKey);

    @ActivityMethod
    void voidPaymentAuthorization(String orderId, String idempotencyKey);
}
