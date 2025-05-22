package com.infra_app.security;

import com.infra_app.exception.CustomUnauthorizedException;
import com.infra_app.model.User;
import com.infra_app.repository.UserRepository;
import com.infra_app.repository.UserTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;
    private final UserTokenRepository tokenRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        try {
            Optional.ofNullable(request.getHeader(AUTH_HEADER))
                    .filter(header -> header.startsWith(TOKEN_PREFIX))
                    .map(header -> header.substring(TOKEN_PREFIX.length()))
                    .ifPresent(token -> {
                        if (tokenRepo.findByTokenAndBlacklistedFalse(token).isEmpty()) {
                            throw new CustomUnauthorizedException("Invalid or revoked token");
                        }

                        jwtUtil.validateToken(token);
                        String username = jwtUtil.extractUsername(token);
                        User user = userRepo.findByUsername(username)
                                .orElseThrow(() -> new CustomUnauthorizedException("User not found"));

                        List<SimpleGrantedAuthority> authorities = jwtUtil.extractRoles(token).stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                user.getUsername(), null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        } catch (CustomUnauthorizedException e) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api/auth") ||
                request.getServletPath().startsWith("/swagger-ui") ||
                request.getServletPath().startsWith("/v3/api-docs") ||
                request.getServletPath().equals("/actuator/health");
    }
}