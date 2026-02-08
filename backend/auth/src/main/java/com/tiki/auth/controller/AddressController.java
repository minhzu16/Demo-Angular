package com.tiki.auth.controller;

import com.tiki.auth.dto.AddressDto;
import com.tiki.auth.service.AddressService;
import com.tiki.auth.exception.BadRequestException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {
    
    @Autowired
    private AddressService addressService;
    
    @GetMapping
    public ResponseEntity<List<AddressDto>> getUserAddresses(
            @RequestHeader("X-User-Id") Long userId) {
        try {
            List<AddressDto> addresses = addressService.getUserAddresses(userId);
            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get addresses: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AddressDto> getAddress(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        try {
            AddressDto address = addressService.getAddress(userId, id);
            return ResponseEntity.ok(address);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get address: " + e.getMessage());
        }
    }
    
    @PostMapping
    public ResponseEntity<AddressDto> createAddress(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddressDto addressDto) {
        try {
            AddressDto created = addressService.createAddress(userId, addressDto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            throw new BadRequestException("Failed to create address: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AddressDto> updateAddress(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody AddressDto addressDto) {
        try {
            AddressDto updated = addressService.updateAddress(userId, id, addressDto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            throw new BadRequestException("Failed to update address: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAddress(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        try {
            addressService.deleteAddress(userId, id);
            return ResponseEntity.ok("Address deleted successfully");
        } catch (Exception e) {
            throw new BadRequestException("Failed to delete address: " + e.getMessage());
        }
    }
    
    @PostMapping("/{id}/set-default")
    public ResponseEntity<AddressDto> setDefaultAddress(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        try {
            AddressDto address = addressService.setDefaultAddress(userId, id);
            return ResponseEntity.ok(address);
        } catch (Exception e) {
            throw new BadRequestException("Failed to set default address: " + e.getMessage());
        }
    }
    
    @GetMapping("/default")
    public ResponseEntity<AddressDto> getDefaultAddress(
            @RequestHeader("X-User-Id") Long userId) {
        try {
            AddressDto address = addressService.getDefaultAddress(userId);
            return ResponseEntity.ok(address);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get default address: " + e.getMessage());
        }
    }
}
