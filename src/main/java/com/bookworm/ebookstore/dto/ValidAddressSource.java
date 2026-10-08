package com.bookworm.ebookstore.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AddressSourceValidator.class)
@Documented
public @interface ValidAddressSource {
    String message() default "Either addressId or shippingAddress must be provided, but not both";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
