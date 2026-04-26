package com.padelPlay.mapper;

import com.padelPlay.dto.request.SiteRequest;
import com.padelPlay.dto.response.SiteResponse;
import com.padelPlay.entity.Site;
import org.springframework.stereotype.Component;

@Component
public class SiteMapper {

    public Site toEntity(SiteRequest request) {
        return Site.builder()
                .nom(request.getNom())
                .adresse(request.getAdresse())
                .heureOuverture(request.getHeureOuverture())
                .heureFermeture(request.getHeureFermeture())
                .dureeMatchMinutes(request.getDureeMatchMinutes())
                .dureeEntreMatchMinutes(request.getDureeEntreMatchMinutes())
                .anneeCivile(request.getAnneeCivile())
                .build();
    }

    public SiteResponse toResponse(Site site) {
        return SiteResponse.builder()
                .id(site.getId())
                .nom(site.getNom())
                .adresse(site.getAdresse())
                .heureOuverture(site.getHeureOuverture())
                .heureFermeture(site.getHeureFermeture())
                .dureeMatchMinutes(site.getDureeMatchMinutes())
                .dureeEntreMatchMinutes(site.getDureeEntreMatchMinutes())
                .anneeCivile(site.getAnneeCivile())
                .build();
    }
}
