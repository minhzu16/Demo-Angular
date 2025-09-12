package demo_tt.demo_tt.service;

import demo_tt.demo_tt.entity.UserEntity;
import demo_tt.demo_tt.model.dto.UpdateProfileRequest;
import demo_tt.demo_tt.model.dto.UserProfileDto;
import demo_tt.demo_tt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {
    
    @Autowired
    private UserRepository userRepository;
    
    public UserProfileDto getUserProfile(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        return convertToDto(user);
    }
    
    public UserProfileDto updateUserProfile(String username, UpdateProfileRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        // Cập nhật thông tin
        user.setFullName(request.getFullName());
        user.setAge(request.getAge());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setGender(request.getGender());
        user.setWorkplace(request.getWorkplace());
        user.setEmail(request.getEmail());
        
        UserEntity savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }
    
    private UserProfileDto convertToDto(UserEntity user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setAge(user.getAge());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAddress(user.getAddress());
        dto.setGender(user.getGender());
        dto.setWorkplace(user.getWorkplace());
        dto.setEmail(user.getEmail());
        return dto;
    }
}
