package com.padelPlay.service.impl;

import com.padelPlay.entity.Membre;
import com.padelPlay.entity.Penalite;
import com.padelPlay.entity.enums.TypeMembre;
import com.padelPlay.exception.BusinessException;
import com.padelPlay.exception.ResourceNotFoundException;
import com.padelPlay.repository.MembreRepository;
import com.padelPlay.repository.PenaliteRepository;
import com.padelPlay.service.MembreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembreServiceImpl implements MembreService {

    private final MembreRepository membreRepository;
    private final PenaliteRepository penaliteRepository;

    @Override
    public Membre create(Membre membre) {
        validateMatricule(membre.getMatricule(), membre.getTypeMembre());

        if (membreRepository.existsByMatricule(membre.getMatricule())) {
            throw new BusinessException("Matricule already exists : " + membre.getMatricule());
        }
        if (membre.getEmail() != null && membreRepository.existsByEmail(membre.getEmail())) {
            throw new BusinessException("Email already exists : " + membre.getEmail());
        }
        if (membre.getTypeMembre() == TypeMembre.SITE && membre.getSite() == null) {
            throw new BusinessException("A SITE member must be linked to a site");
        }

        membre.setSolde(0.0);
        return membreRepository.save(membre);
    }

    @Override
    public Membre getById(Long id) {
        return membreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id : " + id));
    }

    @Override
    public Membre getByMatricule(String matricule) {
        return membreRepository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with matricule : " + matricule));
    }

    @Override
    public List<Membre> getAll() {
        return membreRepository.findAll();
    }

    @Override
    public Membre update(Long id, Membre membre) {
        Membre existing = getById(id);
        existing.setNom(membre.getNom());
        existing.setPrenom(membre.getPrenom());
        existing.setEmail(membre.getEmail());
        return membreRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Membre existing = getById(id);
        membreRepository.delete(existing);
    }

    @Override
    public boolean hasActivePenalty(Long membreId) {
        return penaliteRepository.existsByMembreIdAndDateFinAfter(membreId, LocalDate.now());
    }

    @Override
    public boolean hasOutstandingBalance(Long membreId) {
        Membre membre = getById(membreId);
        return membre.getSolde() > 0.0;
    }

    @Override
    public void addPenalty(Long membreId) {
        Membre membre = getById(membreId);
        Penalite penalite = Penalite.builder()
                .membre(membre)
                .dateFin(LocalDate.now().plusWeeks(1))
                .motif("Private match not filled before deadline")
                .build();
        penaliteRepository.save(penalite);
    }

    private void validateMatricule(String matricule, TypeMembre type) {
        boolean valid = switch (type) {
            case GLOBAL -> matricule.matches("^G\\d{4}$");
            case SITE   -> matricule.matches("^S\\d{5}$");
            case LIBRE  -> matricule.matches("^L\\d{5}$");
        };
        if (!valid) {
            throw new BusinessException("Invalid matricule format for type " + type + " : " + matricule);
        }
    }
}
