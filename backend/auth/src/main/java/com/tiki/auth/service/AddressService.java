package com.tiki.auth.service;

import com.tiki.auth.dto.AddressDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AddressService {
    public List<AddressDto> getUserAddresses() {
        return new ArrayList<>();
    }

    public AddressDto getAddress(Long id) {
        AddressDto dto = new AddressDto();
        dto.setId(id);
        dto.setLabel("Home");
        dto.setJsonAddress("{}");
        dto.setDefault(true);
        return dto;
    }

    public AddressDto createAddress(AddressDto addressDto) { return addressDto; }

    public AddressDto updateAddress(Long id, AddressDto addressDto) { return addressDto; }

    public void deleteAddress(Long id) {}

    public AddressDto setDefaultAddress(Long id) { return getAddress(id); }

    public AddressDto getDefaultAddress() { return getAddress(1L); }
}


