package com.tiki.auth.dto;

import com.tiki.auth.entity.OAuthAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLoginRequest {

    @NotNull(message = "Provider is required")
    private OAuthAccount.Provider provider;

    @NotBlank(message = "Provider user ID is required")
    private String providerUserId;

    private String email;

    private String name;

    private String accessToken;
}
