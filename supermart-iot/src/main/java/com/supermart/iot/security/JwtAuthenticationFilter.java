package com.supermart.iot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Per-request JWT authentication filter.
 *
 * <p>Intercepts every HTTP request and inspects the {@code Authorization} header
 * for a {@code Bearer} token. If a valid, non-expired JWT is found, the filter
 * extracts the subject email, loads the {@link UserDetails}, and populates the
 * {@link org.springframework.security.core.context.SecurityContext} so that
 * downstream filters and controllers see an authenticated principal.</p>
 *
 * <p>Requests without a {@code Bearer} token are passed through unchanged,
 * allowing the security configuration to enforce access rules separately.</p>
 *
 * <p>JWT validation is delegated to {@link JwtService}, which enforces the
 * 45-minute access token expiry configured by SCRUM-3.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Validates the JWT Bearer token from the request and sets the authentication
     * in the {@link org.springframework.security.core.context.SecurityContext}.
     *
     * <p>If the {@code Authorization} header is absent or does not start with
     * {@code Bearer }, the filter chain continues without modification. If a
     * token is present but invalid or expired, the exception is logged and the
     * request proceeds unauthenticated.</p>
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain
     * @throws jakarta.servlet.ServletException if the filter chain processing fails
     * @throws java.io.IOException              if an I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String email = jwtService.extractEmail(jwt);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
