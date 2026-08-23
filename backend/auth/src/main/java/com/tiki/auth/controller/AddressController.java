package com.tiki.auth.controller;

import com.tiki.auth.entity.UserAddress;
import com.tiki.auth.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<UserAddress>> getAddresses(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(addressService.getAddresses(userId));
    }

    @PostMapping
    public ResponseEntity<UserAddress> addAddress(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody UserAddress address) {
        return ResponseEntity.ok(addressService.addAddress(userId, address));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAddress> updateAddress(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @RequestBody UserAddress address) {
        return ResponseEntity.ok(addressService.updateAddress(userId, id, address));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        addressService.deleteAddress(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<Void> setDefault(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        addressService.setDefault(userId, id);
        return ResponseEntity.ok().build();
    }
}
