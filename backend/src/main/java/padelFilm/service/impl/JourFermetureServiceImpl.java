package com.padelPlay.service.impl;

import com.padelPlay.entity.JourFermeture;
import com.padelPlay.exception.ResourceNotFoundException;
import com.padelPlay.repository.JourFermetureRepository;
import com.padelPlay.service.JourFermetureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JourFermetureServiceImpl implements JourFermetureService {

    private final JourFermetureRepository jourFermetureRepository;

    @Override
    public JourFermeture create(JourFermeture jourFermeture) {
        return jourFermetureRepository.save(jourFermeture);
    }

    @Override
    public JourFermeture getById(Long id) {
        return jourFermetureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JourFermeture not found with id: " + id));
    }

    @Override
    public List<JourFermeture> getAll() {
        return jourFermetureRepository.findAll();
    }

    @Override
    public List<JourFermeture> getGlobal() {
        return jourFermetureRepository.findByGlobalTrue();
    }

    @Override
    public List<JourFermeture> getBySite(Long siteId) {
        return jourFermetureRepository.findBySiteId(siteId);
    }

    @Override
    public void delete(Long id) {
        JourFermeture existing = getById(id);
        jourFermetureRepository.delete(existing);
    }
}

