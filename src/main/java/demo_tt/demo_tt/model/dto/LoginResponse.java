package demo_tt.demo_tt.model.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String accessToken;
    private UserProfileDto user;

    public LoginResponse(String accessToken, UserProfileDto user) {
        this.accessToken = accessToken;
        this.user = user;
    }

    public LoginResponse() {
    }
}
