package com.zorvyn.finance.service;

import com.zorvyn.finance.domain.entity.Role;
import com.zorvyn.finance.domain.entity.User;
import com.zorvyn.finance.domain.enums.RoleType;
import com.zorvyn.finance.domain.enums.UserStatus;
import com.zorvyn.finance.dto.request.LoginRequest;
import com.zorvyn.finance.dto.request.RegisterRequest;
import com.zorvyn.finance.dto.response.AuthResponse;
import com.zorvyn.finance.dto.response.UserResponse;
import com.zorvyn.finance.exception.BadRequestException;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.repository.RoleRepository;
import com.zorvyn.finance.repository.UserRepository;
import com.zorvyn.finance.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    public AuthResponse login(LoginRequest request) {
        // Spring Security handles password comparison via the AuthenticationManager
        // if credentials are wrong, it throws BadCredentialsException (caught by GlobalExceptionHandler)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = tokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));

        return new AuthResponse(token, user.getUsername(), user.getRole().getName().name());
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // check uniqueness before anything else
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        // default to VIEWER if no role specified — keeps open registration safe
        // TODO: in production, might want to restrict ADMIN role assignment to existing admins only
        final RoleType roleType;
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try {
                roleType = RoleType.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid role: " + request.getRole()
                        + ". Allowed values: VIEWER, ANALYST, ADMIN");
            }
        } else {
            roleType = RoleType.VIEWER;
        }

        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleType));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }
}
