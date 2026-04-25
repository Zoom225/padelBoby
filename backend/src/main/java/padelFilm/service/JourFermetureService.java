package com.padelPlay.service;

import com.padelPlay.entity.JourFermeture;

import java.util.List;

public interface JourFermetureService {
    JourFermeture create(JourFermeture jourFermeture);
    JourFermeture getById(Long id);
    List<JourFermeture> getAll();
    List<JourFermeture> getGlobal();
    List<JourFermeture> getBySite(Long siteId);
    void delete(Long id);
}

