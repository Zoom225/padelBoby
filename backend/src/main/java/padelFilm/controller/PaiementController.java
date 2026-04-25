package com.padelPlay.controller;

import com.padelPlay.dto.response.PaiementResponse;
import com.padelPlay.mapper.PaiementMapper;
import com.padelPlay.service.PaiementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Endpoints for managing match payments. " +
        "A payment is automatically created with EN_ATTENTE status when a reservation is made. " +
        "Payment lifecycle: " +
        "EN_ATTENTE (created on reservation) → PAYE (payment confirmed) → REMBOURSE (if the reservation is cancelled). " +
        "Business rules: " +
        "- Each match costs €60 divided between 4 players = €15 per player. " +
        "- Payment must be completed before match day. " +
        "- If a member has an outstanding balance, it is automatically added to their next payment. " +
        "- If a player has not paid by the day before the match, their reservation is automatically cancelled " +
        "and the spot is released for other members. " +
        "- If the organizer of a public match ends up covering missing players, " +
        "the unpaid shares are added to their outstanding balance.")
public class PaiementController {

    private final PaiementService paiementService;
    private final PaiementMapper paiementMapper;

    @Operation(
            summary = "Pay for a reservation",
            description = "Processes the payment for a given reservation. " +
                    "Only the member who owns the reservation can complete the payment. " +
                    "Once the payment is processed: " +
                    "1. If the member has an outstanding balance, it is automatically added to the payment amount. " +
                    "2. The outstanding balance is cleared from the member account. " +
                    "3. The payment status changes from EN_ATTENTE to PAYE. " +
                    "4. The reservation status changes from EN_ATTENTE to CONFIRMEE. " +
                    "5. The match player count is recalculated from confirmed reservations. " +
                    "6. If confirmed reservations reach 4, the match status changes to COMPLET. " +
                    "For PUBLIC matches, payment is the validation step — first paid = first served."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment successfully processed and reservation confirmed",
                    content = @Content(schema = @Schema(implementation = PaiementResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payment already done, or member does not own this reservation",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Reservation or payment not found",
                    content = @Content)
    })
    @PostMapping("/reservation/{reservationId}/membre/{membreId}")
    public ResponseEntity<PaiementResponse> pay(
            @Parameter(description = "ID of the reservation to pay for", required = true)
            @PathVariable Long reservationId,
            @Parameter(description = "ID of the member making the payment — must match the reservation owner", required = true)
            @PathVariable Long membreId) {
        return ResponseEntity.ok(
                paiementMapper.toResponse(paiementService.pay(reservationId, membreId))
        );
    }

    @Operation(
            summary = "Get a payment by ID",
            description = "Returns a single payment by its ID. " +
                    "Includes the payment amount, current status, and payment date if already processed. " +
                    "Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found and returned",
                    content = @Content(schema = @Schema(implementation = PaiementResponse.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<PaiementResponse> getById(
            @Parameter(description = "ID of the payment to retrieve", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(paiementMapper.toResponse(paiementService.getById(id)));
    }

    @Operation(
            summary = "Get the payment linked to a reservation",
            description = "Returns the payment associated with a specific reservation. " +
                    "Each reservation has exactly one payment. " +
                    "Useful to check the payment status before allowing a member to join a match. " +
                    "Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found and returned",
                    content = @Content(schema = @Schema(implementation = PaiementResponse.class))),
            @ApiResponse(responseCode = "404", description = "No payment found for this reservation",
                    content = @Content)
    })
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<PaiementResponse> getByReservationId(
            @Parameter(description = "ID of the reservation to retrieve the payment for", required = true)
            @PathVariable Long reservationId) {
        return ResponseEntity.ok(
                paiementMapper.toResponse(paiementService.getByReservationId(reservationId))
        );
    }

    @Operation(
            summary = "Get all payments for a member",
            description = "Returns the full payment history of a member across all their reservations. " +
                    "Includes payments in every status: EN_ATTENTE, PAYE, and REMBOURSE. " +
                    "Useful for the member interface to display payment history " +
                    "and for the admin interface to track revenue by member. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of payments for the member returned successfully",
                    content = @Content(schema = @Schema(implementation = PaiementResponse.class))),
            @ApiResponse(responseCode = "404", description = "Member not found",
                    content = @Content)
    })
    @GetMapping("/membre/{membreId}")
    public ResponseEntity<List<PaiementResponse>> getByMembreId(
            @Parameter(description = "ID of the member to retrieve payments for", required = true)
            @PathVariable Long membreId) {
        List<PaiementResponse> paiements = paiementService.getByMembreId(membreId)
                .stream()
                .map(paiementMapper::toResponse)
                .toList();
        return ResponseEntity.ok(paiements);
    }
}
