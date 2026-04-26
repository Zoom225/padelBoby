package com.padelPlay.dto.request;

import com.padelPlay.entity.enums.TypeMatch;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchRequest {

    @NotNull(message = "Terrain id is required")
    private Long terrainId;

    @NotNull(message = "Organizer id is required")
    private Long organisateurId;

    @NotNull(message = "Date is required")
    @Future(message = "Match date must be in the future")
    private LocalDate date;

    @NotNull(message = "Start time is required")
    private LocalTime heureDebut;

    @NotNull(message = "Match type is required")
    private TypeMatch typeMatch;
}
