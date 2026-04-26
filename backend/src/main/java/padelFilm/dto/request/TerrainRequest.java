package com.padelPlay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerrainRequest {

    @NotBlank(message = "Name is required")
    private String nom;

    @NotNull(message = "Site id is required")
    private Long siteId;
}
