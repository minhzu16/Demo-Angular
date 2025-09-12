package demo_tt.demo_tt.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtils jwtUtils;
	private final UserDetailsService userDetailsService;

	public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
		this.jwtUtils = jwtUtils;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
			throws ServletException, IOException {
		System.out.println("=== JWT Filter Processing: " + request.getMethod() + " " + request.getRequestURI() + " ===");
		try {
			String header = request.getHeader(HttpHeaders.AUTHORIZATION);
			System.out.println("Raw Authorization header: " + header);
			System.out.println("Auth header present: " + (StringUtils.hasText(header) && header.startsWith("Bearer ")));

			if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
				String token = header.substring(7);
				String tokenPreview = token.length() > 10 ? token.substring(0, 10) + "..." : token;
				System.out.println("Processing token (preview): " + tokenPreview);

				if (!jwtUtils.validateJwtToken(token)) {
					System.out.println("Token validation failed - responding 401");
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT");
					return;
				}

				UsernamePasswordAuthenticationToken authentication = getAuthenticationFromToken(token, request);
				if (authentication != null) {
					SecurityContextHolder.getContext().setAuthentication(authentication);
					System.out.println("Authentication set in SecurityContext for user: " + authentication.getName());
				}
			} else {
				System.out.println("No Bearer token present");
			}
			System.out.println("=== JWT Filter Complete ===");
			filterChain.doFilter(request, response);
		} catch (Exception e) {
			System.err.println("Error processing JWT token: " + e.getMessage());
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
			return;
		}
	}

	private UsernamePasswordAuthenticationToken getAuthenticationFromToken(String token, HttpServletRequest request) {
		String username = jwtUtils.getUsernameFromJwt(token);
		if (!StringUtils.hasText(username)) {
			return null;
		}
		UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		System.out.println("UserDetails loaded: " + userDetails.getUsername());
		System.out.println("UserDetails authorities: " + userDetails.getAuthorities());
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				userDetails, null, userDetails.getAuthorities());
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		return authentication;
	}
}


