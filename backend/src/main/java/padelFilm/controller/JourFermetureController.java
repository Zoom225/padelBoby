package com.padelPlay.controller;

import com.padelPlay.dto.request.JourFermetureRequest;
import com.padelPlay.dto.response.JourFermetureResponse;
import com.padelPlay.entity.JourFermeture;
import com.padelPlay.entity.Site;
import com.padelPlay.mapper.JourFermetureMapper;
import com.padelPlay.service.AdminAccessService;
import com.padelPlay.service.JourFermetureService;
import com.padelPlay.service.SiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jours-fermeture")
@RequiredArgsConstructor
@Tag(name = "Jours de fermeture", description = "Gestion des jours de fermeture globaux et par site")
public class JourFermetureController {

    private final JourFermetureService jourFermetureService;
    private final SiteService siteService;
    private final JourFermetureMapper jourFermetureMapper;
    private final AdminAccessService adminAccessService;

    @Operation(summary = "Creer un jour de fermeture", security = @SecurityRequirement(name = "Bearer Auth"))
    @PostMapping
    public ResponseEntity<JourFermetureResponse> create(@Valid @RequestBody JourFermetureRequest request) {
        if (Boolean.TRUE.equals(request.getGlobal())) {
            adminAccessService.assertGlobalAdmin();
        } else if (request.getSiteId() != null) {
            adminAccessService.assertCanAccessSite(request.getSiteId());
        }

        Site site = (request.getSiteId() != null) ? siteService.getById(request.getSiteId()) : null;
        JourFermeture entity = jourFermetureMapper.toEntity(request, site);
        JourFermeture saved = jourFermetureService.create(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(jourFermetureMapper.toResponse(saved));
    }

    @Operation(summary = "Lister tous les jours de fermeture")
    @GetMapping
    public ResponseEntity<List<JourFermetureResponse>> getAll() {
        List<JourFermetureResponse> list = jourFermetureService.getAll()
                .stream()
                .map(jourFermetureMapper::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Lister les jours de fermeture globaux")
    @GetMapping("/global")
    public ResponseEntity<List<JourFermetureResponse>> getGlobal() {
        List<JourFermetureResponse> list = jourFermetureService.getGlobal()
                .stream()
                .map(jourFermetureMapper::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Lister les jours de fermeture d'un site")
    @GetMapping("/site/{siteId}")
    public ResponseEntity<List<JourFermetureResponse>> getBySite(@PathVariable Long siteId) {
        List<JourFermetureResponse> list = jourFermetureService.getBySite(siteId)
                .stream()
                .map(jourFermetureMapper::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Supprimer un jour de fermeture", security = @SecurityRequirement(name = "Bearer Auth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        JourFermeture existing = jourFermetureService.getById(id);
        if (Boolean.TRUE.equals(existing.getGlobal())) {
            adminAccessService.assertGlobalAdmin();
        } else if (existing.getSite() != null) {
            adminAccessService.assertCanAccessSite(existing.getSite().getId());
        }
        jourFermetureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

