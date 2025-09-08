package demo_tt.demo_tt.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtils jwtUtils;
	private final UserDetailsService userDetailsService;

	public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
		this.jwtUtils = jwtUtils;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			String header = request.getHeader(HttpHeaders.AUTHORIZATION);
			System.out.println("Auth header: " + header);

			if (header != null && header.startsWith("Bearer ")) {
				String token = header.substring(7);
				System.out.println("Processing token: " + token);

				if (jwtUtils.validateJwtToken(token)) {
					String username = jwtUtils.getUsernameFromJwt(token);
					System.out.println("Valid token for user: " + username);
					
					UserDetails userDetails = userDetailsService.loadUserByUsername(username);
					UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
							userDetails, null, userDetails.getAuthorities());
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			}
			filterChain.doFilter(request, response);
		} catch (Exception e) {
			System.err.println("Error processing JWT token: " + e.getMessage());
			throw e;
		}
	}
}


