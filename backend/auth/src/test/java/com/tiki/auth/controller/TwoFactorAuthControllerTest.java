package com.tiki.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiki.auth.dto.TwoFactorDtos;
import com.tiki.auth.service.TwoFactorAuthService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TwoFactorAuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TwoFactorAuthService twoFactorService;

    @InjectMocks
    private TwoFactorAuthController twoFactorController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(twoFactorController).build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/2fa/setup returns setup response with secretKey and QR url")
    void testSetup2FA() throws Exception {
        TwoFactorDtos.SetupResponse response = TwoFactorDtos.SetupResponse.builder()
                .secretKey("SECRET123")
                .qrCodeUrl("https://api.qrserver.com/test")
                .totpUri("otpauth://totp/Tiki:test?secret=SECRET123")
                .backupCodes(List.of("CODE-01", "CODE-02"))
                .build();

        when(twoFactorService.setup2FA(10L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/2fa/setup")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secretKey").value("SECRET123"))
                .andExpect(jsonPath("$.backupCodes[0]").value("CODE-01"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/2fa/enable succeeds with valid code")
    void testEnable2FA() throws Exception {
        TwoFactorDtos.VerifyRequest request = TwoFactorDtos.VerifyRequest.builder()
                .code("123456")
                .build();

        when(twoFactorService.enable2FA(10L, "123456")).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/2fa/enable")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(twoFactorService).enable2FA(10L, "123456");
    }

    @Test
    @DisplayName("GET /api/v1/auth/2fa/status returns status")
    void testGetStatus() throws Exception {
        TwoFactorDtos.StatusResponse status = TwoFactorDtos.StatusResponse.builder()
                .enabled(true)
                .remainingBackupCodes(8)
                .build();

        when(twoFactorService.getStatus(10L)).thenReturn(status);

        mockMvc.perform(get("/api/v1/auth/2fa/status")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.remainingBackupCodes").value(8));
    }
}
