package com.zorvyn.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zorvyn.finance.dto.request.LoginRequest;
import com.zorvyn.finance.dto.response.AuthResponse;
import com.zorvyn.finance.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        AuthResponse mockResponse = new AuthResponse("test-jwt-token", "admin", "ADMIN");

        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-jwt-token"))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void login_missingUsername_returnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setPassword("admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
