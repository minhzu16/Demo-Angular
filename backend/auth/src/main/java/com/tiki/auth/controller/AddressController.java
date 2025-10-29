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
    public ResponseEntity<List<AddressDto>> getUserAddresses() {
        try {
            List<AddressDto> addresses = addressService.getUserAddresses();
            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get addresses: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AddressDto> getAddress(@PathVariable Long id) {
        try {
            AddressDto address = addressService.getAddress(id);
            return ResponseEntity.ok(address);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get address: " + e.getMessage());
        }
    }
    
    @PostMapping
    public ResponseEntity<AddressDto> createAddress(@Valid @RequestBody AddressDto addressDto) {
        try {
            AddressDto created = addressService.createAddress(addressDto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            throw new BadRequestException("Failed to create address: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AddressDto> updateAddress(@PathVariable Long id, @Valid @RequestBody AddressDto addressDto) {
        try {
            AddressDto updated = addressService.updateAddress(id, addressDto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            throw new BadRequestException("Failed to update address: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAddress(@PathVariable Long id) {
        try {
            addressService.deleteAddress(id);
            return ResponseEntity.ok("Address deleted successfully");
        } catch (Exception e) {
            throw new BadRequestException("Failed to delete address: " + e.getMessage());
        }
    }
    
    @PostMapping("/{id}/set-default")
    public ResponseEntity<AddressDto> setDefaultAddress(@PathVariable Long id) {
        try {
            AddressDto address = addressService.setDefaultAddress(id);
            return ResponseEntity.ok(address);
        } catch (Exception e) {
            throw new BadRequestException("Failed to set default address: " + e.getMessage());
        }
    }
    
    @GetMapping("/default")
    public ResponseEntity<AddressDto> getDefaultAddress() {
        try {
            AddressDto address = addressService.getDefaultAddress();
            return ResponseEntity.ok(address);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get default address: " + e.getMessage());
        }
    }
}
