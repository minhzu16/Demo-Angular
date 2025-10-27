package com.tiki.common.controller;

import com.tiki.common.dto.AddressDto;
import com.tiki.common.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressDto>> list(@PathVariable Long userId){
        return ResponseEntity.ok(addressService.findByUser(userId));
    }

    @PostMapping
    public ResponseEntity<AddressDto> create(@PathVariable Long userId, @RequestBody AddressDto dto){
        dto.setUserId(userId);
        return ResponseEntity.ok(addressService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressDto> update(@PathVariable Long userId,@PathVariable Integer id,@RequestBody AddressDto dto){
        dto.setUserId(userId);
        return addressService.update(id,dto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        addressService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
