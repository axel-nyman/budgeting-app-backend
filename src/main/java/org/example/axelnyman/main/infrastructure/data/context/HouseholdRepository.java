package org.example.axelnyman.main.infrastructure.data.context;

import org.example.axelnyman.main.domain.model.Household;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HouseholdRepository extends JpaRepository<Household, Long> {
}