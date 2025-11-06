package com.tiki.auth.service;

import com.tiki.auth.dto.ChangePasswordRequest;
import com.tiki.auth.dto.UpdateProfileRequest;
import com.tiki.auth.entity.User;
import com.tiki.auth.entity.UserRole;
import com.tiki.auth.exception.BadRequestException;
import com.tiki.auth.exception.UserNotFoundException;
import com.tiki.auth.repository.UserRepository;
import com.tiki.auth.repository.UserRoleRepository;

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
    private final UserRoleRepository userRoleRepository;
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
        loadUserRoles(user);
        return user;
    }
    
    /**
     * Load roles from user_roles table
     */
    public void loadUserRoles(User user) {
        Set<User.Role> roles = userRoleRepository.findRolesByUserId(user.getId());
        if (!roles.isEmpty()) {
            user.setRoles(roles);
        }
    }
    
    /**
     * Add role to user
     */
    @Transactional
    public void addRoleToUser(Long userId, User.Role role, Long grantedBy) {
        if (!userRoleRepository.existsByUserIdAndRole(userId, role)) {
            UserRole userRole = UserRole.builder()
                    .userId(userId)
                    .role(role)
                    .grantedBy(grantedBy)
                    .build();
            userRoleRepository.save(userRole);
            log.info("Added role {} to user {}", role, userId);
        }
    }
    
    /**
     * Remove role from user
     */
    @Transactional
    public void removeRoleFromUser(Long userId, User.Role role) {
        userRoleRepository.deleteByUserIdAndRole(userId, role);
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
}
