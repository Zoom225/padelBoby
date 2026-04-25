package com.padelPlay.service;

import com.padelPlay.entity.Administrateur;
import com.padelPlay.entity.enums.TypeAdministrateur;
import com.padelPlay.repository.AdministrateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAccessService {

    private final AdministrateurRepository administrateurRepository;

    public Administrateur getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Admin authentication is required");
        }

        return administrateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated admin not found"));
    }

    public void assertGlobalAdmin() {
        Administrateur admin = getCurrentAdmin();
        if (admin.getTypeAdministrateur() != TypeAdministrateur.GLOBAL) {
            throw new AccessDeniedException("Only GLOBAL admin can perform this action");
        }
    }

    public void assertCanAccessSite(Long targetSiteId) {
        Administrateur admin = getCurrentAdmin();

        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return;
        }

        Long adminSiteId = admin.getSite() != null ? admin.getSite().getId() : null;
        if (adminSiteId == null || !adminSiteId.equals(targetSiteId)) {
            throw new AccessDeniedException("SITE admin can only manage their own site");
        }
    }

    public void assertCanManageMember(Long memberSiteId) {
        Administrateur admin = getCurrentAdmin();

        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return;
        }

        Long adminSiteId = admin.getSite() != null ? admin.getSite().getId() : null;
        if (memberSiteId == null || adminSiteId == null || !adminSiteId.equals(memberSiteId)) {
            throw new AccessDeniedException("SITE admin can only manage members from their own site");
        }
    }
}

