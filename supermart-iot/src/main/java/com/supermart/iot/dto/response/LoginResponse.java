package com.supermart.iot.dto.response;

import lombok.*;

/**
 * Response payload returned after a successful login or token refresh.
 *
 * <p>Contains the issued access token, refresh token, token type ({@code Bearer}),
 * and the access token's lifetime in seconds. The {@code expiresIn} value reflects
 * the 45-minute expiry introduced by SCRUM-3 (2700 seconds).</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponse {

    /** The signed JWT access token to be sent in the {@code Authorization: Bearer} header. */
    private String accessToken;

    /** The signed JWT refresh token used to obtain a new access token when it expires. */
    private String refreshToken;

    /**
     * Access token lifetime in seconds.
     * Set to 2700 (45 minutes) per SCRUM-3 security requirement.
     */
    private long expiresIn;

    /** Token scheme — always {@code Bearer} for JWT-based authentication. */
    private String tokenType;
}
