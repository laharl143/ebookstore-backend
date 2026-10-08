package com.bookworm.ebookstore.mapper;

import java.time.OffsetDateTime;

import com.bookworm.ebookstore.dto.AddressRequest;
import com.bookworm.ebookstore.dto.AddressResponse;
import com.bookworm.ebookstore.dto.ShippingAddress;
import com.bookworm.ebookstore.entity.Address;
import com.bookworm.ebookstore.entity.User;

public final class AddressMapper {

    private AddressMapper() {
    }

    public static AddressResponse toResponse(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(
                address.getId(),
                address.getFirstName(),
                address.getLastName(),
                address.getEmail(),
                address.getPhone(),
                address.getStreetAddress(),
                address.getBarangay(),
                address.getCity(),
                address.getProvince(),
                address.getZipCode(),
                address.getCountry(),
                address.isDefault(),
                address.getCreatedAt()
        );
    }

    public static Address toEntity(AddressRequest request, User user, boolean isDefault, OffsetDateTime now) {
        Address address = new Address();
        address.setUser(user);
        address.setFirstName(request.firstName());
        address.setLastName(request.lastName());
        address.setEmail(request.email());
        address.setPhone(request.phone());
        address.setStreetAddress(request.streetAddress());
        address.setBarangay(request.barangay());
        address.setCity(request.city());
        address.setProvince(request.province());
        address.setZipCode(request.zipCode());
        address.setCountry(request.country());
        address.setDefault(isDefault);
        address.setCreatedAt(now);
        return address;
    }

    public static ShippingAddress toShippingAddress(Address address) {
        if (address == null) {
            return null;
        }
        return new ShippingAddress(
                address.getFirstName(),
                address.getLastName(),
                address.getEmail(),
                address.getPhone(),
                address.getStreetAddress(),
                address.getBarangay(),
                address.getCity(),
                address.getProvince(),
                address.getZipCode(),
                address.getCountry()
        );
    }

    public static ShippingAddress toShippingAddress(AddressRequest request) {
        if (request == null) {
            return null;
        }
        return new ShippingAddress(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.streetAddress(),
                request.barangay(),
                request.city(),
                request.province(),
                request.zipCode(),
                request.country()
        );
    }
}
