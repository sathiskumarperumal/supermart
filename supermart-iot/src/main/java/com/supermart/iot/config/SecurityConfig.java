package com.supermart.iot.config;

import com.supermart.iot.repository.UserRepository;
import com.supermart.iot.security.DeviceKeyAuthFilter;
import com.supermart.iot.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration — stateless JWT-based REST API.
 *
 * <p>Configures a {@link SecurityFilterChain} with CSRF disabled, stateless
 * session management, and per-request JWT validation via
 * {@link JwtAuthenticationFilter}. Device-key authentication for IoT telemetry
 * ingestion is handled by {@link DeviceKeyAuthFilter}.</p>
 *
 * <p>Public endpoints: {@code /auth/**}, {@code /h2-console/**},
 * {@code /v3/api-docs/**}, {@code /swagger-ui/**}, {@code /actuator/**}.
 * All other requests require a valid JWT Bearer token.</p>
 *
 * <p>CORS is delegated to {@link CorsConfig} via the
 * {@link org.springframework.web.cors.CorsConfigurationSource} bean.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // JwtAuthenticationFilter is NOT injected here by constructor — doing so
    // creates a cycle: SecurityConfig → JwtAuthenticationFilter → UserDetailsService
    // (a @Bean defined in this class) → SecurityConfig.
    // Instead, it is received as a method parameter in securityFilterChain(),
    // which Spring resolves after all beans are fully initialised.
    private final DeviceKeyAuthFilter deviceKeyAuthFilter;
    private final UserRepository userRepository;

    /**
     * Configures the stateless JWT + device-key security filter chain.
     *
     * <p>CSRF is disabled (stateless API). Session creation policy is
     * {@code STATELESS}. The {@link JwtAuthenticationFilter} is received as a
     * method parameter to avoid a circular-bean dependency.</p>
     *
     * @param http          the {@link HttpSecurity} builder provided by Spring Security
     * @param jwtAuthFilter the JWT authentication filter bean (method-param injection)
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the security configuration cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/telemetry").hasRole("DEVICE")
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(fo -> fo.disable())) // for H2 console
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(deviceKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Provides a {@link UserDetailsService} that loads users by email from the database.
     *
     * @return a {@link UserDetailsService} backed by {@link UserRepository}
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException
     *         if no user is found with the given email
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPasswordHash())
                        .roles(user.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    /**
     * Creates a DAO-backed {@link AuthenticationProvider} using BCrypt password
     * encoding and the {@link UserDetailsService} defined in this configuration.
     *
     * @return a configured {@link AuthenticationProvider}
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} from Spring Security's
     * {@link AuthenticationConfiguration}.
     *
     * @param config the Spring Security authentication configuration
     * @return the global {@link AuthenticationManager}
     * @throws Exception if the manager cannot be retrieved
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Creates a BCrypt {@link PasswordEncoder} for secure password hashing.
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
