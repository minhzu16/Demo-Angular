package com.tiki.auth.service;

import com.tiki.auth.dto.ChangePasswordRequest;
import com.tiki.auth.dto.UpdateProfileRequest;
import com.tiki.auth.entity.User;
import com.tiki.auth.exception.BadRequestException;
import com.tiki.auth.exception.UserNotFoundException;
import com.tiki.auth.entity.UserAddress;
import com.tiki.auth.entity.LoyaltyTransaction;
import com.tiki.auth.repository.LoyaltyTransactionRepository;
import com.tiki.auth.repository.UserRepository;
import com.tiki.auth.repository.UserAddressRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final PasswordEncoder passwordEncoder;
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        log.debug("Getting current user: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
    
    public User getUserById(Long id) {
        log.debug("Getting user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));
        return user;
    }
    
    /**
     * Load roles from user_roles table
     */
    public void loadUserRoles(User user) {
        user.getRoles();
    }
    
    /**
     * Set user role directly
     */
    @Transactional
    public void updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));
        user.setRole(User.Role.valueOf(roleName.toUpperCase()));
        userRepository.save(user);
        log.info("Updated role to {} for user {}", roleName, userId);
    }

    /**
     * Add role to user
     */
    @Transactional
    public void addRoleToUser(Long userId, User.Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));
        user.addRole(role);
        userRepository.save(user);
        log.info("Added role {} to user {}", role, userId);
    }
    
    /**
     * Remove role from user
     */
    @Transactional
    public void removeRoleFromUser(Long userId, User.Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));
        user.removeRole(role);
        userRepository.save(user);
        log.info("Removed role {} from user {}", role, userId);
    }
    
    @Transactional
    public User updateProfile(UpdateProfileRequest request) {
        User currentUser = getCurrentUser();
        
        // Update username if provided and different
        if (request.getUsername() != null && !request.getUsername().equals(currentUser.getUsername())) {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                throw new BadRequestException("Username already exists");
            }
            currentUser.setUsername(request.getUsername());
        }
        
        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().equals(currentUser.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new BadRequestException("Email already exists");
            }
            currentUser.setEmail(request.getEmail());
        }
        
        if (request.getFullName() != null) currentUser.setFullName(request.getFullName());
        if (request.getAge() != null) currentUser.setAge(request.getAge());
        if (request.getPhoneNumber() != null) currentUser.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null) currentUser.setAddress(request.getAddress());
        if (request.getGender() != null) currentUser.setGender(request.getGender());
        if (request.getWorkplace() != null) currentUser.setWorkplace(request.getWorkplace());
        
        return userRepository.save(currentUser);
    }
    
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User currentUser = getCurrentUser();
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        
        // Update to new password
        currentUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
    }
    
    @Transactional
    public void deleteAccount() {
        User currentUser = getCurrentUser();
        
        // Delete user account
        userRepository.delete(currentUser);
    }
    
    @Transactional
    public User updateLoyaltyPoints(Long userId, int points) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));
        
        int currentPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0;
        int newPoints = currentPoints + points;
        
        // Cannot have negative points
        if (newPoints < 0) {
            newPoints = 0;
        }
        
        user.setLoyaltyPoints(newPoints);
        
        // Update Tier
        if (newPoints >= 20000) {
            user.setLoyaltyTier(User.LoyaltyTier.PLATINUM);
        } else if (newPoints >= 5000) {
            user.setLoyaltyTier(User.LoyaltyTier.GOLD);
        } else if (newPoints >= 1000) {
            user.setLoyaltyTier(User.LoyaltyTier.SILVER);
        } else {
            user.setLoyaltyTier(User.LoyaltyTier.BRONZE);
        }
        
        User savedUser = userRepository.save(user);

        // Record transaction
        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .user(savedUser)
                .pointsChange(points)
                .balanceAfter(newPoints)
                .reason(points > 0 ? "ORDER_REWARD" : "REDEMPTION")
                .createdAt(java.time.LocalDateTime.now())
                .build();
        loyaltyTransactionRepository.save(transaction);
        
        return savedUser;
    }

    /**
     * Get loyalty transaction history for a user
     */
    public Page<LoyaltyTransaction> getLoyaltyHistory(Long userId, Pageable pageable) {
        return loyaltyTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    // --- Address Management ---

    public List<UserAddress> getUserAddresses(Long userId) {
        return userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    @Transactional
    public UserAddress addAddress(Long userId, UserAddress address) {
        User user = getUserById(userId);
        address.setUser(user);
        
        if (address.getIsDefault() != null && address.getIsDefault()) {
            resetDefaultAddresses(userId);
        } else if (userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).isEmpty()) {
            address.setIsDefault(true);
        }
        
        return userAddressRepository.save(address);
    }

    @Transactional
    public UserAddress updateAddress(Long userId, Long addressId, UserAddress request) {
        UserAddress address = userAddressRepository.findById(addressId)
            .orElseThrow(() -> new RuntimeException("Address not found"));
            
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        address.setReceiverName(request.getReceiverName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setAddress(request.getAddress());
        
        if (request.getIsDefault() != null && request.getIsDefault() && !address.getIsDefault()) {
            resetDefaultAddresses(userId);
            address.setIsDefault(true);
        }
        
        return userAddressRepository.save(address);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        userAddressRepository.deleteByIdAndUserId(addressId, userId);
    }

    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findById(addressId)
            .orElseThrow(() -> new RuntimeException("Address not found"));
            
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        resetDefaultAddresses(userId);
        address.setIsDefault(true);
        userAddressRepository.save(address);
    }

    private void resetDefaultAddresses(Long userId) {
        List<UserAddress> addresses = userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
        addresses.forEach(a -> {
            if (a.getIsDefault()) {
                a.setIsDefault(false);
                userAddressRepository.save(a);
            }
        });
    }

    // --- Admin Methods ---
    
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    @Transactional
    public void updateUserStatus(Long userId, String status) {
        User user = getUserById(userId);
        user.setStatus(User.UserStatus.valueOf(status.toUpperCase()));
        userRepository.save(user);
        log.info("Updated status to {} for user {}", status, userId);
    }
}
