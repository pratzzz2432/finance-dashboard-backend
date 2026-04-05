package com.zorvyn.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private String username;
    private String role;

    public AuthResponse(String accessToken, String username, String role) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.username = username;
        this.role = role;
    }
}
