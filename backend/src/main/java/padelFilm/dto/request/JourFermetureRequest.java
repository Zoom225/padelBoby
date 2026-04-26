package com.padelPlay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JourFermetureRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    private String raison;

    @NotNull(message = "Global flag is required")
    private Boolean global;

    private Long siteId;
}

