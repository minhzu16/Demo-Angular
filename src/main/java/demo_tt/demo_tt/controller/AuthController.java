package demo_tt.demo_tt.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import demo_tt.demo_tt.model.dto.LoginRequest;
import demo_tt.demo_tt.model.dto.LoginResponse;
import demo_tt.demo_tt.model.dto.UpdateProfileRequest;
import demo_tt.demo_tt.model.dto.UserProfileDto;
import demo_tt.demo_tt.security.JwtUtils;
import demo_tt.demo_tt.service.UserProfileService;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtUtils jwtUtils;
	private final UserProfileService userProfileService;

	public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils, UserProfileService userProfileService) {
		this.authenticationManager = authenticationManager;
		this.jwtUtils = jwtUtils;
		this.userProfileService = userProfileService;
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@Validated @RequestBody LoginRequest request) {
		System.out.println("=== Login Request ===");
		System.out.println("Username: " + request.getUsername());
		System.out.println("Password length: " + (request.getPassword() != null ? request.getPassword().length() : 0));
		
		try {
			UsernamePasswordAuthenticationToken authToken = 
				new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
			
			System.out.println("Created authentication token");
			Authentication authentication = authenticationManager.authenticate(authToken);
			
			System.out.println("Authentication successful");
			System.out.println("User authorities: " + authentication.getAuthorities());
			
			String token = jwtUtils.generateToken(authentication.getName());
			System.out.println("Generated token length: " + token.length());
			
			// Lấy thông tin user profile
			UserProfileDto userProfile = userProfileService.getUserProfile(authentication.getName());
			LoginResponse response = new LoginResponse(token, userProfile);
			return ResponseEntity.ok(response);
			
		} catch (Exception e) {
			System.err.println("=== Authentication Error ===");
			System.err.println("Error type: " + e.getClass().getName());
			System.err.println("Error message: " + e.getMessage());
			e.printStackTrace();
			
			return ResponseEntity
				.status(401)
				.body(new ErrorResponse("Authentication failed: " + e.getMessage()));
		}
	}

	@GetMapping("/check")
	public ResponseEntity<?> checkToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		System.out.println("/api/auth/check header: " + header);
		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7);
			try {
				boolean valid = jwtUtils.validateJwtToken(token);
				if (valid) {
					String username = jwtUtils.getUsernameFromJwt(token);
					return ResponseEntity.ok("OK: " + username);
				}
				return ResponseEntity.status(401).body("Invalid token");
			} catch (Exception e) {
				return ResponseEntity.status(401).body("Error: " + e.getMessage());
			}
		}
		return ResponseEntity.status(400).body("Missing Authorization header");
	}

	@GetMapping("/test")
	public ResponseEntity<?> testEndpoint() {
		return ResponseEntity.ok("Test endpoint works - no auth required");
	}

	@GetMapping("/profile")
	public ResponseEntity<?> getProfile(Authentication authentication) {
		try {
			UserProfileDto profile = userProfileService.getUserProfile(authentication.getName());
			return ResponseEntity.ok(profile);
		} catch (Exception e) {
			return ResponseEntity.status(404).body(new ErrorResponse("User not found: " + e.getMessage()));
		}
	}

	@PutMapping("/profile")
	public ResponseEntity<?> updateProfile(@Validated @RequestBody UpdateProfileRequest request, Authentication authentication) {
		try {
			UserProfileDto updatedProfile = userProfileService.updateUserProfile(authentication.getName(), request);
			return ResponseEntity.ok(updatedProfile);
		} catch (Exception e) {
			return ResponseEntity.status(400).body(new ErrorResponse("Update failed: " + e.getMessage()));
		}
	}
}

class ErrorResponse {
    private String message;
    
    public ErrorResponse(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
}


