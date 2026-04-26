package com.padelPlay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequest {

    @NotNull(message = "Match id is required")
    private Long matchId;

    @NotNull(message = "Member id is required")
    private Long membreId;

    @NotNull(message = "Requester id is required")
    private Long requesterId;
}
