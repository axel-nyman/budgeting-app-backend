package org.example.axelnyman.main.infrastructure.data.context;

import org.example.axelnyman.main.domain.model.HouseholdInvitation;
import org.example.axelnyman.main.domain.model.HouseholdInvitation.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HouseholdInvitationRepository extends JpaRepository<HouseholdInvitation, Long> {

    @Query("SELECT hi FROM HouseholdInvitation hi WHERE hi.household.id = :householdId AND hi.invitedUser.id = :invitedUserId AND hi.status = :status")
    Optional<HouseholdInvitation> findActiveByHouseholdAndInvitedUser(
            @Param("householdId") Long householdId, 
            @Param("invitedUserId") Long invitedUserId, 
            @Param("status") InvitationStatus status);

    Optional<HouseholdInvitation> findByToken(String token);

    @Query("SELECT hi FROM HouseholdInvitation hi " +
           "WHERE hi.invitedUser.id = :invitedUserId " +
           "AND hi.status = :status " +
           "ORDER BY hi.createdAt DESC")
    List<HouseholdInvitation> findByInvitedUserAndStatus(
            @Param("invitedUserId") Long invitedUserId, 
            @Param("status") InvitationStatus status);

    @Modifying
    @Query("UPDATE HouseholdInvitation hi SET hi.status = :expiredStatus, hi.updatedAt = :currentTime " +
           "WHERE hi.status = :pendingStatus AND hi.expiresAt < :currentTime")
    int updateExpiredInvitationsToExpired(
            @Param("expiredStatus") InvitationStatus expiredStatus,
            @Param("pendingStatus") InvitationStatus pendingStatus,
            @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT hi FROM HouseholdInvitation hi " +
           "WHERE hi.invitedUser.id = :invitedUserId " +
           "AND hi.status = :status " +
           "AND hi.expiresAt > :currentTime " +
           "ORDER BY hi.createdAt DESC")
    List<HouseholdInvitation> findPendingNonExpiredByInvitedUser(
            @Param("invitedUserId") Long invitedUserId,
            @Param("status") InvitationStatus status,
            @Param("currentTime") LocalDateTime currentTime);
}