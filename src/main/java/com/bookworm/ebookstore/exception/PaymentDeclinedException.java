package com.bookworm.ebookstore.exception;

public class PaymentDeclinedException extends RuntimeException {

    private final String transactionId;

    public PaymentDeclinedException(String transactionId) {
        super("Card declined");
        this.transactionId = transactionId;
    }

    public ApiErrorCode getErrorCode() {
        return ApiErrorCode.PAYMENT_DECLINED;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
