package com.padelPlay.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerrainResponse {
    private Long id;
    private String nom;
    private Long siteId;
    private String siteNom;
}
