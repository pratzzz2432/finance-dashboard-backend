package com.zorvyn.finance.service;

import com.zorvyn.finance.domain.entity.Role;
import com.zorvyn.finance.domain.entity.User;
import com.zorvyn.finance.domain.enums.RoleType;
import com.zorvyn.finance.domain.enums.UserStatus;
import com.zorvyn.finance.dto.request.UpdateUserRequest;
import com.zorvyn.finance.dto.response.UserResponse;
import com.zorvyn.finance.exception.BadRequestException;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.repository.RoleRepository;
import com.zorvyn.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::fromEntity);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserResponse.fromEntity(user);
    }

    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null) {
            if (userRepository.existsByEmail(request.getEmail()) &&
                    !user.getEmail().equals(request.getEmail())) {
                throw new BadRequestException("Email '" + request.getEmail() + "' is already in use");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getRole() != null) {
            try {
                RoleType roleType = RoleType.valueOf(request.getRole().toUpperCase());
                Role role = roleRepository.findByName(roleType)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleType));
                user.setRole(role);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid role: " + request.getRole());
            }
        }

        if (request.getStatus() != null) {
            try {
                user.setStatus(UserStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + request.getStatus()
                        + ". Allowed values: ACTIVE, INACTIVE");
            }
        }

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    // deactivate instead of delete — keeps user data for references in existing records
    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }
}
