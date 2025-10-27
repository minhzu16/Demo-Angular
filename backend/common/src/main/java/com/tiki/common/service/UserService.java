package com.tiki.common.service;

import com.tiki.common.dto.UserDto;
import com.tiki.common.entity.UserEntity;
import com.tiki.common.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private UserDto toDto(UserEntity entity) {
        UserDto dto = new UserDto();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setRole(entity.getRole());
        return dto;
    }

    public List<UserDto> findAll() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Optional<UserDto> findById(Long id) {
        return userRepository.findById(id).map(this::toDto);
    }

    public Optional<UserDto> update(Long id, UserDto request) {
        return userRepository.findById(id).map(entity -> {
            if (request.getUsername() != null && !request.getUsername().isBlank()) {
                entity.setUsername(request.getUsername());
            }
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                entity.setEmail(request.getEmail());
            }
            if (request.getRole() != null && !request.getRole().isBlank()) {
                entity.setRole(request.getRole());
            }
            userRepository.save(entity);
            return toDto(entity);
        });
    }
}
