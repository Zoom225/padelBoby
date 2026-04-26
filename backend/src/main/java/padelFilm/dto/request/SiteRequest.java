package com.padelPlay.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteRequest {

    @NotBlank(message = "Name is required")
    private String nom;

    @NotBlank(message = "Address is required")
    private String adresse;

    @NotNull(message = "Opening time is required")
    private LocalTime heureOuverture;

    @NotNull(message = "Closing time is required")
    private LocalTime heureFermeture;

    @NotNull(message = "Match duration is required")
    @Min(value = 1, message = "Match duration must be at least 1 minute")
    private Integer dureeMatchMinutes;

    @NotNull(message = "Break duration is required")
    @Min(value = 0, message = "Break duration must be positive")
    private Integer dureeEntreMatchMinutes;

    @NotNull(message = "Civil year is required")
    private Integer anneeCivile;
}
