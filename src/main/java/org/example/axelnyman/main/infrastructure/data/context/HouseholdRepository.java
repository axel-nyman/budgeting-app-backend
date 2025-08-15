package org.example.axelnyman.main.infrastructure.data.context;

import org.example.axelnyman.main.domain.model.Household;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HouseholdRepository extends JpaRepository<Household, Long> {
    
    @Query("SELECT h FROM Household h LEFT JOIN FETCH h.users u WHERE h.id = :id AND (u.deletedAt IS NULL OR u IS NULL)")
    Optional<Household> findByIdWithActiveMembers(@Param("id") Long id);
}