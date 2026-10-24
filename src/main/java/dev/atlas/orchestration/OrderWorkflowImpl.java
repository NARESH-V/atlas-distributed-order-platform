package dev.atlas.orchestration;

import dev.atlas.order.OrderStatus;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.UUID;

public class OrderWorkflowImpl implements OrderWorkflow {
    private final OrderActivities activities = Workflow.newActivityStub(
            OrderActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofMillis(200))
                            .setMaximumInterval(Duration.ofSeconds(2))
                            .setMaximumAttempts(3)
                            .build())
                    .build());

    @Override
    public OrderWorkflowResult run(CreateOrderCommand command) {
        validate(command);

        String orderId = command.orderId().toString();
        boolean inventoryReserved = false;
        boolean paymentAuthorized = false;

        try {
            activities.reserveInventory(command.sku(), command.quantity(), orderId + ":reserve");
            inventoryReserved = true;

            activities.authorizePayment(orderId, command.amountInMinorUnits(),
                    command.currency(), orderId + ":authorize");
            paymentAuthorized = true;

            activities.requestShipment(orderId, orderId + ":ship");
            return new OrderWorkflowResult(command.orderId(), OrderStatus.COMPLETED,
                    "Order orchestration completed.");
        } catch (Exception failure) {
            boolean compensationSucceeded = true;

            if (paymentAuthorized) {
                try {
                    activities.voidPaymentAuthorization(orderId, orderId + ":void");
                } catch (Exception compensationFailure) {
                    compensationSucceeded = false;
                    Workflow.getLogger(OrderWorkflowImpl.class).error(
                            "Payment compensation failed for order " + orderId, compensationFailure);
                }
            }

            if (inventoryReserved) {
                try {
                    activities.releaseInventory(command.sku(), command.quantity(), orderId + ":release");
                } catch (Exception compensationFailure) {
                    compensationSucceeded = false;
                    Workflow.getLogger(OrderWorkflowImpl.class).error(
                            "Inventory compensation failed for order " + orderId, compensationFailure);
                }
            }

            String message = compensationSucceeded
                    ? "Order failed; completed steps were compensated."
                    : "Order failed and one or more compensating actions also failed.";
            return new OrderWorkflowResult(command.orderId(), OrderStatus.FAILED, message);
        }
    }

    private void validate(CreateOrderCommand command) {
        if (command == null || command.orderId() == null) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (command.sku() == null || command.sku().isBlank()) {
            throw new IllegalArgumentException("sku is required");
        }
        if (command.quantity() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (command.amountInMinorUnits() <= 0) {
            throw new IllegalArgumentException("amountInMinorUnits must be positive");
        }
        if (command.currency() == null || !command.currency().matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currency must be a three-letter uppercase code");
        }
    }
}
