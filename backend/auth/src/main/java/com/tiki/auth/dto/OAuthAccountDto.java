package com.tiki.auth.dto;

import com.tiki.auth.entity.OAuthAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthAccountDto {
    private Long id;
    private Long userId;
    private OAuthAccount.Provider provider;
    private String providerUserId;
    private String email;
    private LocalDateTime createdAt;
}
