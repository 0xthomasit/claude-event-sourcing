package com.example.user.interfaces.rest;

import com.example.user.application.dto.*;
import com.example.user.application.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User profile and address management")
public class UserController {

    private final UserService userService;

    // ─── Profile ──────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create user profile (called by auth-service after registration)")
    public ResponseEntity<ProfileResponse> createProfile(
            @Valid @RequestBody CreateProfileRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createProfile(req));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get profile by user ID")
    public ResponseEntity<ProfileResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping("/by-auth/{authId}")
    @Operation(summary = "Get profile by auth service user ID")
    public ResponseEntity<ProfileResponse> getByAuthId(@PathVariable String authId) {
        return ResponseEntity.ok(userService.getByAuthId(authId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile")
    public ResponseEntity<ProfileResponse> updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(userService.updateProfile(id, req));
    }

    // ─── Addresses ────────────────────────────────────────────────────────────

    @GetMapping("/{userId}/addresses")
    @Operation(summary = "Get all addresses for a user")
    public ResponseEntity<List<AddressResponse>> getAddresses(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getAddresses(userId));
    }

    @PostMapping("/{userId}/addresses")
    @Operation(summary = "Add a new address")
    public ResponseEntity<AddressResponse> addAddress(
            @PathVariable UUID userId,
            @Valid @RequestBody AddressRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.addAddress(userId, req));
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable UUID userId,
            @PathVariable UUID addressId) {
        userService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/addresses/{addressId}/default")
    @Operation(summary = "Set address as default")
    public ResponseEntity<AddressResponse> setDefault(
            @PathVariable UUID userId,
            @PathVariable UUID addressId) {
        return ResponseEntity.ok(userService.setDefaultAddress(userId, addressId));
    }
}
