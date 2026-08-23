package com.tiki.auth.service;

import com.tiki.auth.entity.User;
import com.tiki.auth.entity.UserAddress;
import com.tiki.auth.repository.UserAddressRepository;
import com.tiki.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;

    public List<UserAddress> getAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    @Transactional
    public UserAddress addAddress(Long userId, UserAddress address) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        address.setUser(user);
        
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            resetDefault(userId);
        } else if (addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).isEmpty()) {
            address.setIsDefault(true);
        }
        
        return addressRepository.save(address);
    }

    @Transactional
    public UserAddress updateAddress(Long userId, Long addressId, UserAddress updatedAddress) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (Boolean.TRUE.equals(updatedAddress.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            resetDefault(userId);
        }
        
        address.setReceiverName(updatedAddress.getReceiverName());
        address.setPhoneNumber(updatedAddress.getPhoneNumber());
        address.setAddress(updatedAddress.getAddress());
        address.setIsDefault(updatedAddress.getIsDefault());
        
        return addressRepository.save(address);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);
        
        if (wasDefault) {
            List<UserAddress> remaining = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                UserAddress newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }

    @Transactional
    public void setDefault(Long userId, Long addressId) {
        resetDefault(userId);
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        address.setIsDefault(true);
        addressRepository.save(address);
    }

    private void resetDefault(Long userId) {
        List<UserAddress> defaults = addressRepository.findByUserIdAndIsDefaultTrue(userId);
        for (UserAddress ad : defaults) {
            ad.setIsDefault(false);
            addressRepository.save(ad);
        }
    }
}
