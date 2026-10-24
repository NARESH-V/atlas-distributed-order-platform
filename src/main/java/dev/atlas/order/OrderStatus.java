package dev.atlas.order;

public enum OrderStatus {
    ACCEPTED,
    INVENTORY_RESERVED,
    PAYMENT_AUTHORIZED,
    SHIPMENT_REQUESTED,
    COMPLETED,
    COMPENSATING,
    FAILED,
    CANCELLED
}
