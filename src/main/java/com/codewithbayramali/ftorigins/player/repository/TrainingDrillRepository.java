package com.codewithbayramali.ftorigins.player.repository;

import com.codewithbayramali.ftorigins.player.model.TrainingDrill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TrainingDrillRepository extends JpaRepository<TrainingDrill, UUID> {

    /** Active drills for the catalog, effects fetch-joined to avoid N+1. */
    @Query("select distinct d from TrainingDrill d left join fetch d.effects "
            + "where d.active = true order by d.sortOrder")
    List<TrainingDrill> findActiveWithEffects();
}
