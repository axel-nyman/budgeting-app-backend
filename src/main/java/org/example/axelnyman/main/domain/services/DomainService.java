package org.example.axelnyman.main.domain.services;

import java.util.List;
import java.util.Optional;

import org.example.axelnyman.main.domain.abstracts.IDataService;
import org.example.axelnyman.main.domain.abstracts.IDomainService;
import org.example.axelnyman.main.domain.dtos.UserDtos.*;
import org.example.axelnyman.main.domain.dtos.HouseholdDtos.*;
import org.example.axelnyman.main.domain.extensions.UserExtensions;
import org.example.axelnyman.main.domain.extensions.HouseholdExtensions;
import org.example.axelnyman.main.domain.model.Household;
import org.example.axelnyman.main.domain.model.HouseholdInvitation;
import org.example.axelnyman.main.domain.model.User;
import org.example.axelnyman.main.shared.exceptions.HouseholdNotFoundException;
import org.example.axelnyman.main.shared.exceptions.InvitationAlreadyExistsException;
import org.example.axelnyman.main.shared.exceptions.UserAlreadyInHouseholdException;
import org.example.axelnyman.main.shared.exceptions.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DomainService implements IDomainService {

    private final IDataService dataService;

    public DomainService(IDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public Optional<UserResponse> getUserByIdInHousehold(Long id, Long householdId) {
        return dataService.getActiveUserByIdAndHouseholdId(id, householdId)
                .map(UserExtensions::toResponse);
    }

    @Override
    public boolean deleteUser(Long id) {
        return dataService.deleteUserById(id);
    }

    @Override
    public Optional<UserResponse> getUserProfile(Long userId) {
        return dataService.getUserById(userId)
                .map(UserExtensions::toResponse);
    }

    @Override
    public List<UserResponse> getHouseholdUsers(Long householdId) {
        return dataService.getActiveUsersByHouseholdId(householdId)
                .stream()
                .map(UserExtensions::toResponse)
                .toList();
    }

    @Override
    public Optional<HouseholdResponse> getHouseholdDetails(Long householdId) {
        return dataService.getHouseholdWithActiveMembers(householdId)
                .map(HouseholdExtensions::toResponse);
    }

    @Override
    public HouseholdUpdateResponse updateHouseholdName(Long householdId, String name) {
        // Get household
        Household household = dataService.getHouseholdById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException("Household not found"));

        // Update name (trim for consistent formatting)
        household.setName(name.trim());
        
        // Save and return
        Household savedHousehold = dataService.saveHousehold(household);
        return HouseholdExtensions.toUpdateResponse(savedHousehold);
    }

    @Override
    public InvitationResponse createHouseholdInvitation(Long householdId, Long invitedByUserId, String email) {
        validateInvitationRequest(householdId, invitedByUserId, email);
        HouseholdInvitation invitation = buildInvitation(householdId, invitedByUserId, email);
        return saveAndReturnInvitation(invitation);
    }

    private void validateInvitationRequest(Long householdId, Long invitedByUserId, String email) {
        // Find user by email
        User invitedUser = dataService.findActiveUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with this email not found"));

        // Check if user is already in the same household
        if (invitedUser.getHousehold() != null && invitedUser.getHousehold().getId().equals(householdId)) {
            throw new UserAlreadyInHouseholdException("User already belongs to your household");
        }

        // Check for existing active invitation
        Optional<HouseholdInvitation> existingInvitation = dataService.findActiveInvitationByHouseholdAndUser(householdId, invitedUser.getId());
        if (existingInvitation.isPresent()) {
            throw new InvitationAlreadyExistsException("Active invitation already exists for this user");
        }
    }

    private HouseholdInvitation buildInvitation(Long householdId, Long invitedByUserId, String email) {
        // Get required entities
        User invitedUser = dataService.findActiveUserByEmail(email).orElseThrow();
        Household household = dataService.getHouseholdById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException("Household not found"));
        User invitedByUser = dataService.getUserById(invitedByUserId)
                .orElseThrow(() -> new UserNotFoundException("Inviting user not found"));

        // Create invitation entity
        return HouseholdExtensions.toInvitationEntity(household, invitedUser, invitedByUser);
    }

    private InvitationResponse saveAndReturnInvitation(HouseholdInvitation invitation) {
        HouseholdInvitation savedInvitation = dataService.saveHouseholdInvitation(invitation);
        return HouseholdExtensions.toInvitationResponse(savedInvitation);
    }

    @Override
    @Transactional
    public List<InvitationResponse> getUserPendingInvitations(Long userId) {
        // First, expire any outdated invitations
        dataService.expireOutdatedInvitations();
        
        // Then fetch only pending non-expired invitations
        return dataService.getPendingNonExpiredInvitationsForUser(userId)
                .stream()
                .map(HouseholdExtensions::toInvitationResponse)
                .toList();
    }
}
