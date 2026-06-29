package com.example.user.application.service;

import com.example.user.application.dto.*;
import com.example.user.domain.model.UserAddress;
import com.example.user.domain.model.UserProfile;
import com.example.user.domain.repository.UserAddressRepository;
import com.example.user.domain.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserProfileRepository profileRepository;
    private final UserAddressRepository addressRepository;

    // ─── Profile ──────────────────────────────────────────────────────────────

    @Transactional
    public ProfileResponse createProfile(CreateProfileRequest req) {
        if (profileRepository.existsByEmail(req.getEmail()))
            throw new IllegalStateException("Email already registered: " + req.getEmail());
        if (profileRepository.existsByAuthId(req.getAuthId()))
            throw new IllegalStateException("Profile already exists for authId: " + req.getAuthId());

        UserProfile profile = UserProfile.builder()
                .authId(req.getAuthId())
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .avatarUrl(req.getAvatarUrl())
                .build();

        profileRepository.save(profile);
        log.info("Created profile for authId={}", req.getAuthId());
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getByAuthId(String authId) {
        UserProfile profile = profileRepository.findByAuthId(authId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for authId: " + authId));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getById(UUID id) {
        UserProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + id));
        return toResponse(profile);
    }

    @Transactional
    public ProfileResponse updateProfile(UUID id, UpdateProfileRequest req) {
        UserProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + id));
        profile.setFullName(req.getFullName());
        profile.setPhone(req.getPhone());
        profile.setAvatarUrl(req.getAvatarUrl());
        profileRepository.save(profile);
        return toResponse(profile);
    }

    // ─── Addresses ────────────────────────────────────────────────────────────

    @Transactional
    public AddressResponse addAddress(UUID userId, AddressRequest req) {
        UserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + userId));

        if (req.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }

        UserAddress address = UserAddress.builder()
                .user(profile)
                .label(req.getLabel() != null ? req.getLabel() : "HOME")
                .recipientName(req.getRecipientName())
                .phone(req.getPhone())
                .street(req.getStreet())
                .ward(req.getWard())
                .district(req.getDistrict())
                .province(req.getProvince())
                .isDefault(req.isDefault())
                .build();

        addressRepository.save(address);
        log.info("Added address for userId={}", userId);
        return toAddressResponse(address);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(UUID userId) {
        return addressRepository.findByUserId(userId)
                .stream().map(this::toAddressResponse).toList();
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
        if (!address.getUser().getId().equals(userId))
            throw new IllegalStateException("Address does not belong to user: " + userId);
        addressRepository.delete(address);
    }

    @Transactional
    public AddressResponse setDefaultAddress(UUID userId, UUID addressId) {
        addressRepository.clearDefaultForUser(userId);
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
        address.setDefault(true);
        addressRepository.save(address);
        return toAddressResponse(address);
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private ProfileResponse toResponse(UserProfile p) {
        List<AddressResponse> addresses = p.getAddresses() == null ? List.of() :
                p.getAddresses().stream().map(this::toAddressResponse).toList();
        return ProfileResponse.builder()
                .id(p.getId())
                .authId(p.getAuthId())
                .fullName(p.getFullName())
                .email(p.getEmail())
                .phone(p.getPhone())
                .avatarUrl(p.getAvatarUrl())
                .status(p.getStatus())
                .addresses(addresses)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private AddressResponse toAddressResponse(UserAddress a) {
        return AddressResponse.builder()
                .id(a.getId())
                .label(a.getLabel())
                .recipientName(a.getRecipientName())
                .phone(a.getPhone())
                .street(a.getStreet())
                .ward(a.getWard())
                .district(a.getDistrict())
                .province(a.getProvince())
                .isDefault(a.isDefault())
                .build();
    }
}
