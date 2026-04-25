package com.padelPlay.service.impl;

import com.padelPlay.config.JwtConfig;
import com.padelPlay.dto.request.LoginRequest;
import com.padelPlay.dto.response.LoginResponse;
import com.padelPlay.entity.Administrateur;
import com.padelPlay.exception.BusinessException;
import com.padelPlay.repository.AdministrateurRepository;
import com.padelPlay.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AdministrateurRepository administrateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    @Override
    public LoginResponse login(LoginRequest request) {
        // chercher l'admin par email
        Administrateur admin = administrateurRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        // vérifier le password
        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new BusinessException("Invalid credentials");
        }

        // générer le token JWT
        String token = jwtConfig.generateToken(
                admin.getEmail(),
                admin.getTypeAdministrateur().name()
        );

        log.info("Admin {} logged in successfully", admin.getEmail());

        return LoginResponse.builder()
                .token(token)
                .email(admin.getEmail())
                .nom(admin.getNom())
                .prenom(admin.getPrenom())
                .role(admin.getTypeAdministrateur())
                .siteId(admin.getSite() != null ? admin.getSite().getId() : null)
                .build();
    }
}
