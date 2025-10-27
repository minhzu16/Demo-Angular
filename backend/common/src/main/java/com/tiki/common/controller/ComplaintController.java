package com.tiki.common.controller;

import com.tiki.common.dto.ComplaintDto;
import com.tiki.common.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @PostMapping
    public ResponseEntity<ComplaintDto> create(@Valid @RequestBody ComplaintDto dto){
        return ResponseEntity.ok(complaintService.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<ComplaintDto>> getAll(){
        return ResponseEntity.ok(complaintService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintDto> getById(@PathVariable Long id){
        return complaintService.getById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<ComplaintDto>> getByBuyer(@PathVariable Long buyerId){
        return ResponseEntity.ok(complaintService.getByBuyer(buyerId));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<ComplaintDto>> getBySeller(@PathVariable Long sellerId){
        return ResponseEntity.ok(complaintService.getBySeller(sellerId));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ComplaintDto> resolve(@PathVariable Long id,
                                                @RequestParam boolean accepted,
                                                @RequestBody String resolution){
        return complaintService.resolve(id,resolution,accepted)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
