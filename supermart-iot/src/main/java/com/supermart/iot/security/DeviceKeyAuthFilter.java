package com.supermart.iot.security;

import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.repository.IotDeviceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceKeyAuthFilter extends OncePerRequestFilter {

    private final IotDeviceRepository deviceRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String deviceKey = request.getHeader("X-Device-Key");

        if (deviceKey != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<IotDevice> device = deviceRepository.findByDeviceKey(deviceKey);
            if (device.isPresent()) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                "device:" + device.get().getDeviceId(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_DEVICE")));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Device authenticated: {}", device.get().getDeviceSerial());
            }
        }

        filterChain.doFilter(request, response);
    }
}
