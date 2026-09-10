package com.tiki.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiki.auth.dto.AuthResponse;
import com.tiki.auth.dto.OAuthAccountDto;
import com.tiki.auth.dto.OAuthLoginRequest;
import com.tiki.auth.entity.OAuthAccount;
import com.tiki.auth.service.OAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OAuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OAuthService oauthService;

    @InjectMocks
    private OAuthController oauthController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(oauthController).build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/oauth/login returns AuthResponse")
    void testLoginWithOAuth() throws Exception {
        OAuthLoginRequest request = OAuthLoginRequest.builder()
                .provider(OAuthAccount.Provider.GOOGLE)
                .providerUserId("sub-12345")
                .email("user@gmail.com")
                .build();

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(10L, "user", "user@gmail.com", "BUYER");
        AuthResponse authResponse = new AuthResponse("token-abc", "refresh-xyz", userInfo);

        when(oauthService.loginOrRegister(any(OAuthLoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-abc"))
                .andExpect(jsonPath("$.user.username").value("user"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/oauth/accounts returns linked accounts")
    void testGetLinkedAccounts() throws Exception {
        OAuthAccountDto dto = OAuthAccountDto.builder()
                .id(1L)
                .userId(10L)
                .provider(OAuthAccount.Provider.GOOGLE)
                .email("user@gmail.com")
                .build();

        when(oauthService.getLinkedAccounts(10L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/auth/oauth/accounts")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("GOOGLE"));
    }

    @Test
    @DisplayName("DELETE /api/v1/auth/oauth/accounts/{provider} unlinks provider")
    void testUnlinkOAuth() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/oauth/accounts/GOOGLE")
                        .header("X-User-Id", 10L))
                .andExpect(status().isNoContent());

        verify(oauthService).unlinkOAuth(10L, OAuthAccount.Provider.GOOGLE);
    }
}
