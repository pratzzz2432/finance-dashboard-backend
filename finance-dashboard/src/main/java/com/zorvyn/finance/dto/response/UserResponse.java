package com.zorvyn.finance.dto.response;

import com.zorvyn.finance.domain.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private LocalDateTime createdAt;

    public static UserResponse fromEntity(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().getName().name());
        response.setStatus(user.getStatus().name());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
