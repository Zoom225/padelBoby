package com.padelPlay.controller;

import com.padelPlay.dto.request.TerrainRequest;
import com.padelPlay.dto.response.TerrainResponse;
import com.padelPlay.entity.Terrain;
import com.padelPlay.mapper.TerrainMapper;
import com.padelPlay.service.AdminAccessService;
import com.padelPlay.service.TerrainService;
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
@RequestMapping("/api/terrains")
@RequiredArgsConstructor
@Tag(name = "Courts", description = "Endpoints for managing padel courts. Each court belongs to a site and can host matches. A site can have multiple courts with different availabilities.")
public class TerrainController {

    private final TerrainService terrainService;
    private final TerrainMapper terrainMapper;
    private final AdminAccessService adminAccessService;

    @Operation(
            summary = "Create a new court",
            description = "Creates a new padel court and links it to an existing site. The site must exist before creating a court. Requires ADMIN role.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Court successfully created",
                    content = @Content(schema = @Schema(implementation = TerrainResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body or validation error",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — admin token required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Site not found",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<TerrainResponse> create(@Valid @RequestBody TerrainRequest request) {
        adminAccessService.assertCanAccessSite(request.getSiteId());
        Terrain terrain = terrainMapper.toEntity(request);
        Terrain saved = terrainService.create(terrain, request.getSiteId());
        return ResponseEntity.status(HttpStatus.CREATED).body(terrainMapper.toResponse(saved));
    }

    @Operation(
            summary = "Get all courts",
            description = "Returns the list of all padel courts across all sites. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of courts returned successfully",
                    content = @Content(schema = @Schema(implementation = TerrainResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<TerrainResponse>> getAll() {
        List<TerrainResponse> terrains = terrainService.getAll()
                .stream()
                .map(terrainMapper::toResponse)
                .toList();
        return ResponseEntity.ok(terrains);
    }

    @Operation(
            summary = "Get a court by ID",
            description = "Returns a single court by its ID including the site it belongs to. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Court found and returned",
                    content = @Content(schema = @Schema(implementation = TerrainResponse.class))),
            @ApiResponse(responseCode = "404", description = "Court not found",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<TerrainResponse> getById(
            @Parameter(description = "ID of the court to retrieve", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(terrainMapper.toResponse(terrainService.getById(id)));
    }

    @Operation(
            summary = "Get all courts by site",
            description = "Returns all courts belonging to a specific site. Useful for displaying available courts when a member wants to book a match on a given site. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of courts for the given site returned successfully",
                    content = @Content(schema = @Schema(implementation = TerrainResponse.class))),
            @ApiResponse(responseCode = "404", description = "Site not found",
                    content = @Content)
    })
    @GetMapping("/site/{siteId}")
    public ResponseEntity<List<TerrainResponse>> getBySiteId(
            @Parameter(description = "ID of the site to retrieve courts for", required = true)
            @PathVariable Long siteId) {
        List<TerrainResponse> terrains = terrainService.getBySiteId(siteId)
                .stream()
                .map(terrainMapper::toResponse)
                .toList();
        return ResponseEntity.ok(terrains);
    }

    @Operation(
            summary = "Update a court",
            description = "Updates the name of an existing court. The site cannot be changed after creation. Requires ADMIN role.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Court successfully updated",
                    content = @Content(schema = @Schema(implementation = TerrainResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body or validation error",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — admin token required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Court not found",
                    content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<TerrainResponse> update(
            @Parameter(description = "ID of the court to update", required = true)
            @PathVariable Long id,
            @Valid @RequestBody TerrainRequest request) {
        Terrain existing = terrainService.getById(id);
        adminAccessService.assertCanAccessSite(existing.getSite().getId());
        Terrain terrain = terrainMapper.toEntity(request);
        Terrain updated = terrainService.update(id, terrain);
        return ResponseEntity.ok(terrainMapper.toResponse(updated));
    }

    @Operation(
            summary = "Delete a court",
            description = "Permanently deletes a court and all its associated matches. This action is irreversible. Requires ADMIN role.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Court successfully deleted"),
            @ApiResponse(responseCode = "403", description = "Access denied — admin token required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Court not found",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the court to delete", required = true)
            @PathVariable Long id) {
        Terrain existing = terrainService.getById(id);
        adminAccessService.assertCanAccessSite(existing.getSite().getId());
        terrainService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
