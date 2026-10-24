package dev.atlas.orchestration;

import dev.atlas.order.OrderStatus;

import java.util.UUID;

public record OrderWorkflowResult(UUID orderId, OrderStatus status, String message) {
}
