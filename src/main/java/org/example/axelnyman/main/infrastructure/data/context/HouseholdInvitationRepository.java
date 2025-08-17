package org.example.axelnyman.main.infrastructure.data.context;

import org.example.axelnyman.main.domain.model.HouseholdInvitation;
import org.example.axelnyman.main.domain.model.HouseholdInvitation.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HouseholdInvitationRepository extends JpaRepository<HouseholdInvitation, Long> {

    @Query("SELECT hi FROM HouseholdInvitation hi WHERE hi.household.id = :householdId AND hi.invitedUser.id = :invitedUserId AND hi.status = :status")
    Optional<HouseholdInvitation> findActiveByHouseholdAndInvitedUser(
            @Param("householdId") Long householdId, 
            @Param("invitedUserId") Long invitedUserId, 
            @Param("status") InvitationStatus status);

    Optional<HouseholdInvitation> findByToken(String token);
}