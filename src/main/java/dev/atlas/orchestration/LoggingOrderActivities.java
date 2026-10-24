package dev.atlas.orchestration;

import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingOrderActivities implements OrderActivities {
    private static final Logger log = LoggerFactory.getLogger(LoggingOrderActivities.class);

    /*
     * Demonstration adapter only. These methods log intended operations; they do not
     * reserve real inventory, charge a payment method, or create a shipment.
     * Replace this adapter with database/provider-backed idempotent implementations.
     */

    @Override
    public void reserveInventory(String sku, int quantity, String idempotencyKey) {
        log.info("DEMO reserve inventory sku={} quantity={} key={} activity={}",
                sku, quantity, idempotencyKey, Activity.getExecutionContext().getInfo().getActivityId());
    }

    @Override
    public void authorizePayment(String orderId, long amountInMinorUnits, String currency, String idempotencyKey) {
        log.info("DEMO authorize payment orderId={} amountMinor={} currency={} key={}",
                orderId, amountInMinorUnits, currency, idempotencyKey);
    }

    @Override
    public void requestShipment(String orderId, String idempotencyKey) {
        log.info("DEMO request shipment orderId={} key={}", orderId, idempotencyKey);
    }

    @Override
    public void releaseInventory(String sku, int quantity, String idempotencyKey) {
        log.info("DEMO release inventory sku={} quantity={} key={}", sku, quantity, idempotencyKey);
    }

    @Override
    public void voidPaymentAuthorization(String orderId, String idempotencyKey) {
        log.info("DEMO void payment authorization orderId={} key={}", orderId, idempotencyKey);
    }
}
