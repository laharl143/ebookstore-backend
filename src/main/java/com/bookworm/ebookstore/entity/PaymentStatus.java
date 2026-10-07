package com.bookworm.ebookstore.entity;

/** Outcome of one payment attempt. Stored as a string matching the CHECK list in V1. */
public enum PaymentStatus {
    SUCCESS, FAILED, REFUNDED
}
