package com.tiki.common.service;

import com.tiki.common.dto.AddressDto;
import com.tiki.common.entity.AddressEntity;
import com.tiki.common.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    private AddressDto toDto(AddressEntity e){
        AddressDto d = new AddressDto();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setLabel(e.getLabel());
        d.setJsonAddress(e.getJsonAddress());
        d.setIsDefault(e.getIsDefault());
        return d;
    }

    private AddressEntity toEntity(AddressDto d){
        AddressEntity e = new AddressEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setLabel(d.getLabel());
        e.setJsonAddress(d.getJsonAddress());
        e.setIsDefault(Boolean.TRUE.equals(d.getIsDefault()));
        return e;
    }

    public List<AddressDto> findByUser(Long userId){
        return addressRepository.findByUserId(userId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public Optional<AddressDto> find(Integer id){
        return addressRepository.findById(id).map(this::toDto);
    }

    public AddressDto create(AddressDto dto){
        AddressEntity saved = addressRepository.save(toEntity(dto));
        return toDto(saved);
    }

    public Optional<AddressDto> update(Integer id, AddressDto dto){
        return addressRepository.findById(id).map(e -> {
            e.setLabel(dto.getLabel());
            e.setJsonAddress(dto.getJsonAddress());
            e.setIsDefault(dto.getIsDefault());
            return toDto(addressRepository.save(e));
        });
    }

    public void delete(Integer id){
        addressRepository.deleteById(id);
    }
}
