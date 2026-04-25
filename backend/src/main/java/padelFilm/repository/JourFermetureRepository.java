package com.padelPlay.repository;

import com.padelPlay.entity.JourFermeture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JourFermetureRepository extends JpaRepository<JourFermeture, Long> {

    List<JourFermeture> findByGlobalTrue();

    List<JourFermeture> findBySiteId(Long siteId);
}
