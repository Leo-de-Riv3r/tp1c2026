package com.tacs.tp1c2026.services;


import com.tacs.tp1c2026.entities.profiles.Profile;
import com.tacs.tp1c2026.entities.profiles.ProfileGroup;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.Suggestion;
import com.tacs.tp1c2026.properties.ProfileProperties;
import com.tacs.tp1c2026.repositories.ProfileGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ProfileService {

    private final ProfileGroupRepository profileGroupRepository;
    private final UserService userService;
    private final ProfileProperties properties;

    public ProfileService(ProfileGroupRepository profileGroupRepository, UserService userService, ProfileProperties properties){
        this.profileGroupRepository = profileGroupRepository;
        this.userService = userService;
        this.properties = properties;
    }

    @Transactional
    public void updateSuggestionsForUsers() {

        List<ProfileGroup> groups = this.profileGroupRepository.findAll();
        List<User> users = userService.getAll();

        for (User user : users) {

            List<User> closesMatchingUsers = groups.stream()
                    .sorted(Comparator.comparingInt(g -> Profile.complement(g.getRepresentativeProfile(), user.getProfile())))
                    .limit(this.properties.getProfileGroupsToCheck())
                    .flatMap(g -> g.getNeighbours().stream())
                    .limit(this.properties.getMaximumNumberOfUsersToSuggest())
                    .toList();

            List<Suggestion> suggestions = closesMatchingUsers.stream().map(u ->
                            new Suggestion(
                                    u,
                                    user.missingCardsItCanGetFrom(u)
                            )
                    ).toList();

            user.updateSuggestions(suggestions);

        }

        for (ProfileGroup pfg : groups) {
            pfg.updateVector();
        }
        this.profileGroupRepository.saveAll(groups);

    }

    @Transactional
    public void updateProfileGroups(User user){
        List<ProfileGroup> pfg = this.profileGroupRepository.findAll();

        for (ProfileGroup p : pfg) {
            p.removeNeighbor(user);
        }

        List<ProfileGroup> newGroups = pfg.stream()
                .sorted(Comparator.comparingInt(g -> Profile.agreement(g.getRepresentativeProfile(), user.getProfile())))
                .limit(this.properties.getMaximumNumberOfGroupsUserCanBeIn())
                .toList();

        newGroups.forEach(g -> g.addNeighbor(user));

        this.profileGroupRepository.saveAll(pfg);
    }


}
