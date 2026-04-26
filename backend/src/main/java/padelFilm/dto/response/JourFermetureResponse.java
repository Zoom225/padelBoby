package com.padelPlay.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JourFermetureResponse {

    private Long id;
    private LocalDate date;
    private String raison;
    private Boolean global;
    private Long siteId;
    private String siteNom;
}

