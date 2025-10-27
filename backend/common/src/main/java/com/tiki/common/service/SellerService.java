package com.tiki.common.service;

import com.tiki.common.dto.SellerDto;
import com.tiki.common.entity.SellerEntity;
import com.tiki.common.repository.SellerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SellerService {

    @Autowired
    private SellerRepository sellerRepo;

    private SellerDto toDto(SellerEntity e){
        SellerDto d = new SellerDto();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setShopName(e.getShopName());
        d.setPhone(e.getPhone());
        d.setAddress(e.getAddress());
        d.setReturnPolicy(e.getReturnPolicy());
        d.setShippingPolicy(e.getShippingPolicy());
        return d;
    }

    private SellerEntity toEntity(SellerDto d){
        SellerEntity e = new SellerEntity();
        e.setId(d.getId()); // may be null for create
        e.setUserId(d.getUserId());
        e.setShopName(d.getShopName());
        e.setPhone(d.getPhone());
        e.setAddress(d.getAddress());
        e.setReturnPolicy(d.getReturnPolicy());
        e.setShippingPolicy(d.getShippingPolicy());
        return e;
    }

    public List<SellerDto> getAll(){
        return sellerRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public Optional<SellerDto> getById(Long id){
        return sellerRepo.findById(id).map(this::toDto);
    }

    public Optional<SellerDto> getByUserId(Long userId){
        return sellerRepo.findByUserId(userId).map(this::toDto);
    }

    public SellerDto create(SellerDto dto){
        SellerEntity e = toEntity(dto);
        e.setId(null);
        return toDto(sellerRepo.save(e));
    }

    public Optional<SellerDto> update(Long id, SellerDto dto){
        return sellerRepo.findById(id).map(existing -> {
            existing.setShopName(dto.getShopName());
            existing.setPhone(dto.getPhone());
            existing.setAddress(dto.getAddress());
            existing.setReturnPolicy(dto.getReturnPolicy());
            existing.setShippingPolicy(dto.getShippingPolicy());
            return toDto(sellerRepo.save(existing));
        });
    }

    public void delete(Long id){
        sellerRepo.deleteById(id);
    }
}
