package com.bookworm.ebookstore.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AddressSourceValidator implements ConstraintValidator<ValidAddressSource, CreateOrderRequest> {

    @Override
    public boolean isValid(CreateOrderRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        boolean hasAddressId = request.addressId() != null;
        boolean hasShippingAddress = request.shippingAddress() != null;

        // Exactly one must be present
        if ((hasAddressId && !hasShippingAddress) || (!hasAddressId && hasShippingAddress)) {
            return true;
        }

        // Customise violation property to addressId
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("addressId")
                .addConstraintViolation();
        return false;
    }
}
