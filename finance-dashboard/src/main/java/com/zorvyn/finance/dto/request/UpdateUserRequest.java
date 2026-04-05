package com.zorvyn.finance.dto.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class UpdateUserRequest {

    @Size(max = 80, message = "Full name must not exceed 80 characters")
    private String fullName;

    @Email(message = "Please provide a valid email address")
    private String email;

    private String role;

    private String status;
}
