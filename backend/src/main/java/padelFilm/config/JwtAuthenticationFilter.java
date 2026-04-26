package com.padelPlay.config;

import com.padelPlay.entity.Administrateur;
import com.padelPlay.repository.AdministrateurRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;
    private final AdministrateurRepository administrateurRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // si pas de token → on laisse passer (les routes publiques sont gérées dans SecurityConfig)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // enlève "Bearer "

        if (!jwtConfig.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtConfig.extractEmail(token);
        String role = jwtConfig.extractRole(token);

        // vérifier que l'admin existe toujours en BDD
        Administrateur admin = administrateurRepository.findByEmail(email).orElse(null);
        if (admin == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // dire à Spring Security qui est authentifié
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Admin {} authenticated with role {}", email, role);

        filterChain.doFilter(request, response);
    }
}
