package com.bookworm.ebookstore.entity;

/** Order lifecycle; transitions are guarded single statement updates. Stored as a string matching the CHECK list in V1. */
public enum OrderStatus {
    PENDING_PAYMENT, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}
