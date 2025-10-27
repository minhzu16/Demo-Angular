package com.tiki.common.controller;

import com.tiki.common.dto.SellerDto;
import com.tiki.common.service.SellerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sellers")
@CrossOrigin
public class SellerController {

    @Autowired
    private SellerService sellerService;

    @GetMapping
    public ResponseEntity<List<SellerDto>> getAll(){
        return ResponseEntity.ok(sellerService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SellerDto> getById(@PathVariable Long id){
        return sellerService.getById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<SellerDto> getByUserId(@PathVariable Long userId){
        return sellerService.getByUserId(userId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SellerDto> create(@Valid @RequestBody SellerDto dto){
        return ResponseEntity.ok(sellerService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SellerDto> update(@PathVariable Long id, @Valid @RequestBody SellerDto dto){
        return sellerService.update(id,dto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        sellerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
