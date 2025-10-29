package com.tiki.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    private String secret = "mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong12345678";
    private Long expiration = 900000L; // 15 minutes
    private Long refreshExpiration = 604800000L; // 7 days

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    
    public Long getExpiration() { return expiration; }
    public void setExpiration(Long expiration) { this.expiration = expiration; }
    
    public Long getRefreshExpiration() { return refreshExpiration; }
    public void setRefreshExpiration(Long refreshExpiration) { this.refreshExpiration = refreshExpiration; }
}
