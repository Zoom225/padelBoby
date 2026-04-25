package com.padelPlay.controller;

import com.padelPlay.dto.request.MembreRequest;
import com.padelPlay.dto.response.MembreResponse;
import com.padelPlay.entity.Membre;
import com.padelPlay.entity.Site;
import com.padelPlay.mapper.MembreMapper;
import com.padelPlay.service.AdminAccessService;
import com.padelPlay.service.MembreService;
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
@RequestMapping("/api/membres")
@RequiredArgsConstructor
@Tag(name = "Members", description = "Endpoints for managing padel members. " +
        "There are 3 membership types: GLOBAL (matricule starts with G, booking up to 3 weeks ahead, all sites), " +
        "SITE (matricule starts with S, booking up to 2 weeks ahead, own site only), " +
        "and LIBRE (matricule starts with L, booking up to 5 days ahead, all sites). " +
        "Members authenticate only through their matricule — no password is required for member access.")
public class MembreController {

    private final MembreService membreService;
    private final SiteService siteService;
    private final MembreMapper membreMapper;
    private final AdminAccessService adminAccessService;

    @Operation(
            summary = "Register a new member",
            description = "Creates a new member with a unique matricule. " +
                    "The matricule format is validated according to the membership type: " +
                    "GLOBAL → G followed by 4 digits (e.g. G1234), " +
                    "SITE → S followed by 5 digits (e.g. S12345), " +
                    "LIBRE → L followed by 5 digits (e.g. L12345). " +
                    "A SITE member must provide a siteId. GLOBAL and LIBRE members do not need one."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Member successfully registered",
                    content = @Content(schema = @Schema(implementation = MembreResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body, wrong matricule format, or matricule already exists",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Site not found (when siteId is provided)",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<MembreResponse> create(@Valid @RequestBody MembreRequest request) {
        Membre membre = membreMapper.toEntity(request);

        if (request.getSiteId() != null) {
            Site site = siteService.getById(request.getSiteId());
            membre.setSite(site);
        }

        Membre saved = membreService.create(membre);
        return ResponseEntity.status(HttpStatus.CREATED).body(membreMapper.toResponse(saved));
    }

    @Operation(
            summary = "Get all members",
            description = "Returns the list of all registered members across all sites. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of members returned successfully",
                    content = @Content(schema = @Schema(implementation = MembreResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<MembreResponse>> getAll() {
        List<MembreResponse> membres = membreService.getAll()
                .stream()
                .map(membreMapper::toResponse)
                .toList();
        return ResponseEntity.ok(membres);
    }

    @Operation(
            summary = "Get a member by ID",
            description = "Returns a member using their internal ID. " +
                    "Includes the membership type, site information, and current outstanding balance. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Member found and returned",
                    content = @Content(schema = @Schema(implementation = MembreResponse.class))),
            @ApiResponse(responseCode = "404", description = "Member not found",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<MembreResponse> getById(
            @Parameter(description = "Internal ID of the member", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(membreMapper.toResponse(membreService.getById(id)));
    }

    @Operation(
            summary = "Get a member by matricule",
            description = "Returns a member using their unique matricule. " +
                    "This is the main way to identify a member, since authentication is done directly with the matricule. " +
                    "Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Member found and returned",
                    content = @Content(schema = @Schema(implementation = MembreResponse.class))),
            @ApiResponse(responseCode = "404", description = "Member not found with given matricule",
                    content = @Content)
    })
    @GetMapping("/matricule/{matricule}")
    public ResponseEntity<MembreResponse> getByMatricule(
            @Parameter(description = "Unique matricule of the member (e.g. G1234, S12345, L12345)", required = true)
            @PathVariable String matricule) {
        return ResponseEntity.ok(membreMapper.toResponse(membreService.getByMatricule(matricule)));
    }

    @Operation(
            summary = "Check whether a member has an active penalty",
            description = "Returns true if the member is currently under an active penalty. " +
                    "A penalty is applied when a member organizes a private match that is still incomplete 24 hours before match day. " +
                    "During the penalty period (7 days), the member cannot create or join matches. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Penalty status returned",
                    content = @Content(schema = @Schema(implementation = Boolean.class))),
            @ApiResponse(responseCode = "404", description = "Member not found",
                    content = @Content)
    })
    @GetMapping("/{id}/penalty")
    public ResponseEntity<Boolean> hasActivePenalty(
            @Parameter(description = "ID of the member to check", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(membreService.hasActivePenalty(id));
    }

    @Operation(
            summary = "Check whether a member has an outstanding balance",
            description = "Returns true if the member has an outstanding balance. " +
                    "An outstanding balance is generated when a member organizes a public match that does not become complete — " +
                    "the organizer must then cover the missing players' share. " +
                    "A member with an outstanding balance cannot create a new match until it is cleared. " +
                    "The balance is automatically added to the next payment. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance status returned",
                    content = @Content(schema = @Schema(implementation = Boolean.class))),
            @ApiResponse(responseCode = "404", description = "Member not found",
                    content = @Content)
    })
    @GetMapping("/{id}/balance")
    public ResponseEntity<Boolean> hasOutstandingBalance(
            @Parameter(description = "ID of the member to check", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(membreService.hasOutstandingBalance(id));
    }

    @Operation(
            summary = "Update a member",
            description = "Updates the personal information of an existing member (last name, first name, email). " +
                    "The matricule and membership type cannot be changed after registration. Requires ADMIN role.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Member successfully updated",
                    content = @Content(schema = @Schema(implementation = MembreResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body or validation error",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — admin token required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Member not found",
                    content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<MembreResponse> update(
            @Parameter(description = "ID of the member to update", required = true)
            @PathVariable Long id,
            @Valid @RequestBody MembreRequest request) {
        Membre existing = membreService.getById(id);
        Long existingSiteId = existing.getSite() != null ? existing.getSite().getId() : null;
        adminAccessService.assertCanManageMember(existingSiteId);

        Membre membre = membreMapper.toEntity(request);
        Membre updated = membreService.update(id, membre);
        return ResponseEntity.ok(membreMapper.toResponse(updated));
    }

    @Operation(
            summary = "Delete a member",
            description = "Permanently deletes a member and all associated reservations and penalties. " +
                    "This action is irreversible. Requires ADMIN role.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member successfully deleted"),
            @ApiResponse(responseCode = "403", description = "Access denied — admin token required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Member not found",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the member to delete", required = true)
            @PathVariable Long id) {
        Membre existing = membreService.getById(id);
        Long existingSiteId = existing.getSite() != null ? existing.getSite().getId() : null;
        adminAccessService.assertCanManageMember(existingSiteId);

        membreService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
