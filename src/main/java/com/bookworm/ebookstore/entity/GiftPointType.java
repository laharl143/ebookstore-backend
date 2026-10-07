package com.bookworm.ebookstore.entity;

/** Kind of gift points ledger row; EARNED and RESTORED are credits, the others debits. Stored as a string matching the CHECK list in V1. */
public enum GiftPointType {
    EARNED, REDEEMED, RESTORED, REVERSED
}
