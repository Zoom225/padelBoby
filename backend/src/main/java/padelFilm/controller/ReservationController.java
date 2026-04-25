package com.padelPlay.controller;

import com.padelPlay.dto.request.ReservationRequest;
import com.padelPlay.dto.response.ReservationResponse;
import com.padelPlay.entity.Reservation;
import com.padelPlay.mapper.ReservationMapper;
import com.padelPlay.service.ReservationService;
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
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Endpoints for managing match reservations. " +
        "A reservation links a member to a match and automatically generates a pending payment. " +
        "Reservation lifecycle: " +
        "EN_ATTENTE (created, payment pending) → CONFIRMEE (payment done) → ANNULEE (cancelled). " +
        "Business rules: " +
        "- A member cannot reserve a spot in the same match twice. " +
        "- A member with an active penalty cannot make a reservation. " +
        "- A member with an outstanding balance cannot make a reservation. " +
        "- For PRIVATE matches, only the organizer can add players. " +
        "- For PUBLIC matches, any eligible member can join by paying (first come, first served). " +
        "- A SITE member can only reserve on their own site.")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationMapper reservationMapper;

    @Operation(
            summary = "Create a new reservation",
            description = "Creates a reservation for a member on a specific match. " +
                    "A pending payment (EN_ATTENTE) is automatically created alongside the reservation. " +
                    "The reservation is not confirmed until the payment is completed via POST /api/paiements. " +
                    "Business rules enforced: " +
                    "1. The match must not be full (confirmed reservations < 4) and must not be cancelled. " +
                    "2. The member must not already be registered in this match. " +
                    "3. The member must not have an active penalty. " +
                    "4. The member must not have an outstanding balance. " +
                    "5. For PRIVATE matches, only the organizer can add players. " +
                    "6. A SITE member can only book on their own site."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reservation successfully created with a pending payment",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Business rule violation — match full, already registered, penalty, balance, wrong site, or private match restriction",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Match or member not found",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        Reservation reservation = reservationService.create(
                request.getMatchId(),
                request.getMembreId(),
                request.getRequesterId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationMapper.toResponse(reservation));
    }

    @Operation(
            summary = "Get a reservation by ID",
            description = "Returns a single reservation by its ID. " +
                    "Includes the associated match details, member information, reservation status, " +
                    "and the linked payment with its current status. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation found and returned",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reservation not found",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(
            @Parameter(description = "ID of the reservation to retrieve", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(
                reservationMapper.toResponse(reservationService.getById(id))
        );
    }

    @Operation(
            summary = "Get all reservations for a match",
            description = "Returns all reservations linked to a specific match. " +
                    "Useful for the admin interface to see who has joined a match " +
                    "and the payment status of each player. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of reservations for the match returned successfully",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Match not found",
                    content = @Content)
    })
    @GetMapping("/match/{matchId}")
    public ResponseEntity<List<ReservationResponse>> getByMatchId(
            @Parameter(description = "ID of the match to retrieve reservations for", required = true)
            @PathVariable Long matchId) {
        List<ReservationResponse> reservations = reservationService.getByMatchId(matchId)
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
        return ResponseEntity.ok(reservations);
    }

    @Operation(
            summary = "Get all reservations for a member",
            description = "Returns all reservations made by a specific member across all matches. " +
                    "Includes past and upcoming matches with their payment status. " +
                    "Used by the member interface to display their reservation history. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of reservations for the member returned successfully",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Member not found",
                    content = @Content)
    })
    @GetMapping("/membre/{membreId}")
    public ResponseEntity<List<ReservationResponse>> getByMembreId(
            @Parameter(description = "ID of the member to retrieve reservations for", required = true)
            @PathVariable Long membreId) {
        List<ReservationResponse> reservations = reservationService.getByMembreId(membreId)
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
        return ResponseEntity.ok(reservations);
    }

    @Operation(
            summary = "Cancel a reservation",
            description = "Cancels an existing reservation and frees up the spot in the match. " +
                    "If the payment was already completed (PAYE), it is automatically marked as REMBOURSE. " +
                    "The match player count is recalculated automatically from confirmed reservations. " +
                    "If the match was COMPLET, it returns to PLANIFIE status after cancellation, " +
                    "making it joinable again by other members.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reservation successfully cancelled"),
            @ApiResponse(responseCode = "400", description = "Reservation is already cancelled",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — admin token required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Reservation not found",
                    content = @Content)
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @Parameter(description = "ID of the reservation to cancel", required = true)
            @PathVariable Long id) {
        reservationService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
