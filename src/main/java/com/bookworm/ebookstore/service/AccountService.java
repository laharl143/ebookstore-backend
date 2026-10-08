package com.bookworm.ebookstore.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookworm.ebookstore.dto.AddressRequest;
import com.bookworm.ebookstore.dto.AddressResponse;
import com.bookworm.ebookstore.dto.UserResponse;
import com.bookworm.ebookstore.entity.Address;
import com.bookworm.ebookstore.entity.User;
import com.bookworm.ebookstore.exception.AuthenticationException;
import com.bookworm.ebookstore.mapper.AddressMapper;
import com.bookworm.ebookstore.mapper.UserMapper;
import com.bookworm.ebookstore.repository.AddressRepository;
import com.bookworm.ebookstore.repository.UserRepository;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final Clock clock;

    public AccountService(UserRepository userRepository, AddressRepository addressRepository, Clock clock) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
        return UserMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findByUserIdWithCustomSort(userId).stream()
                .map(AddressMapper::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        boolean hasPriorAddresses = addressRepository.existsByUserId(userId);
        boolean isDefault;

        if (!hasPriorAddresses) {
            isDefault = true;
        } else {
            isDefault = Boolean.TRUE.equals(request.isDefault());
            if (isDefault) {
                addressRepository.clearDefaultAddressesForUser(userId);
            }
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        Address address = AddressMapper.toEntity(request, user, isDefault, now);
        Address saved = addressRepository.save(address);

        return AddressMapper.toResponse(saved);
    }
}
