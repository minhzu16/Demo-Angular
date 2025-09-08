package demo_tt.demo_tt.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import demo_tt.demo_tt.model.dto.LoginRequest;
import demo_tt.demo_tt.model.dto.LoginResponse;
import demo_tt.demo_tt.security.JwtUtils;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtUtils jwtUtils;

	public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
		this.authenticationManager = authenticationManager;
		this.jwtUtils = jwtUtils;
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
			
			LoginResponse response = new LoginResponse(token);
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


