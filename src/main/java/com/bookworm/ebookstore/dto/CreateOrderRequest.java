package com.bookworm.ebookstore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@ValidAddressSource
public record CreateOrderRequest(
        Long addressId,
        @Valid AddressRequest shippingAddress,
        Boolean saveAddress,
        @Min(0) Integer pointsToRedeem
) {
    public CreateOrderRequest {
        if (saveAddress == null) {
            saveAddress = false;
        }
        if (pointsToRedeem == null) {
            pointsToRedeem = 0;
        }
    }
}
