package com.padelPlay.mapper;

import com.padelPlay.dto.request.JourFermetureRequest;
import com.padelPlay.dto.response.JourFermetureResponse;
import com.padelPlay.entity.JourFermeture;
import com.padelPlay.entity.Site;
import org.springframework.stereotype.Component;

@Component
public class JourFermetureMapper {

    public JourFermeture toEntity(JourFermetureRequest request, Site site) {
        return JourFermeture.builder()
                .date(request.getDate())
                .raison(request.getRaison())
                .global(request.getGlobal())
                .site(site)
                .build();
    }

    public JourFermetureResponse toResponse(JourFermeture jourFermeture) {
        return JourFermetureResponse.builder()
                .id(jourFermeture.getId())
                .date(jourFermeture.getDate())
                .raison(jourFermeture.getRaison())
                .global(jourFermeture.getGlobal())
                .siteId(jourFermeture.getSite() != null ? jourFermeture.getSite().getId() : null)
                .siteNom(jourFermeture.getSite() != null ? jourFermeture.getSite().getNom() : null)
                .build();
    }
}

