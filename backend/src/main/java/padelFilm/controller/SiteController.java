package com.padelPlay.controller;

import com.padelPlay.dto.request.SiteRequest;
import com.padelPlay.dto.response.SiteResponse;
import com.padelPlay.entity.Site;
import com.padelPlay.mapper.SiteMapper;
import com.padelPlay.service.AdminAccessService;
import com.padelPlay.service.SiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
@Tag(name = "Sites", description = "Endpoints for managing padel sites. A site contains multiple courts and defines its own opening hours, match duration, and closing days.")
public class SiteController {

    private final SiteService siteService;
    private final SiteMapper siteMapper;
    private final AdminAccessService adminAccessService;

    @Operation(
            summary = "Create a new site",
            description = "Creates a new padel site with its configuration (opening hours, match duration, break duration). Requires ADMIN role.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Site successfully created",
                    content = @Content(schema = @Schema(implementation = SiteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body or validation error",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — admin token required",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<SiteResponse> create(@Valid @RequestBody SiteRequest request) {
        adminAccessService.assertGlobalAdmin();
        Site site = siteMapper.toEntity(request);
        Site saved = siteService.create(site);
        return ResponseEntity.status(HttpStatus.CREATED).body(siteMapper.toResponse(saved));
    }

    @Operation(
            summary = "Get all sites",
            description = "Returns the list of all padel sites. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of sites returned successfully",
                    content = @Content(schema = @Schema(implementation = SiteResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<SiteResponse>> getAll() {
        List<SiteResponse> sites = siteService.getAll()
                .stream()
                .map(siteMapper::toResponse)
                .toList();
        return ResponseEntity.ok(sites);
    }

    @Operation(
            summary = "Get a site by ID",
            description = "Returns a single site by its ID. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Site found and returned",
                    content = @Content(schema = @Schema(implementation = SiteResponse.class))),
            @ApiResponse(responseCode = "404", description = "Site not found",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<SiteResponse> getById(
            @Parameter(description = "ID of the site to retrieve", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(siteMapper.toResponse(siteService.getById(id)));
    }

    @Operation(
            summary = "Update a site",
            description = "Updates all fields of an existing site. Requires ADMIN role.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Site successfully updated",
                    content = @Content(schema = @Schema(implementation = SiteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body or validation error",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — admin token required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Site not found",
                    content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<SiteResponse> update(
            @Parameter(description = "ID of the site to update", required = true)
            @PathVariable Long id,
            @Valid @RequestBody SiteRequest request) {
        adminAccessService.assertCanAccessSite(id);
        Site site = siteMapper.toEntity(request);
        Site updated = siteService.update(id, site);
        return ResponseEntity.ok(siteMapper.toResponse(updated));
    }

    @Operation(
            summary = "Delete a site",
            description = "Permanently deletes a site and all its associated courts and closing days. Requires ADMIN role.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Site successfully deleted"),
            @ApiResponse(responseCode = "403", description = "Access denied — admin token required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Site not found",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the site to delete", required = true)
            @PathVariable Long id) {
        adminAccessService.assertCanAccessSite(id);
        siteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
