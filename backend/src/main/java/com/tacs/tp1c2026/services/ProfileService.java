 package com.tacs.tp1c2026.services;


 import com.tacs.tp1c2026.entities.profiles.Profile;
 import com.tacs.tp1c2026.entities.profiles.ProfileGroup;
 import com.tacs.tp1c2026.entities.user.User;

 import com.tacs.tp1c2026.entities.user.embebbed.Suggestion;
 import com.tacs.tp1c2026.properties.ProfileProperties;

 import com.tacs.tp1c2026.repositories.ProfileGroupRepository;
 import java.util.Comparator;
 import java.util.List;
 import org.springframework.stereotype.Service;

 @Service
 public class ProfileService {

   private final ProfileGroupRepository profileGroupRepository;
   private final UsersService userService;
   private final ProfileProperties properties;

   public ProfileService(ProfileGroupRepository profileGroupRepository, UsersService userService, ProfileProperties properties){
     this.profileGroupRepository = profileGroupRepository;
     this.userService = userService;
     this.properties = properties;
   }

   // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
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

   // @Transactional // TODO: rehabilitar cuando Mongo corra como replica set
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
