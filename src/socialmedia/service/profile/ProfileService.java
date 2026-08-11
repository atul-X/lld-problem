package socialmedia.service.profile;

import socialmedia.model.Profile;
import socialmedia.model.ProfileRequest;

import java.util.HashMap;
import java.util.Map;

public class ProfileService {
    private static volatile ProfileService instance;
    private final Map<Integer, Profile> profilesMap = new HashMap<>();

    private ProfileService() {
    }

    public static ProfileService getProfileService(){
        if (instance == null){
            synchronized (ProfileService.class){
                if (instance == null){
                    instance = new ProfileService();
                }
            }
        }
        return instance;
    }

    public synchronized Profile addProfile(ProfileRequest profileRequest){
        boolean mobileTaken = profilesMap.values().stream()
                .anyMatch(p -> p.getMobileno().equals(profileRequest.getMobileno()));
        if (mobileTaken){
            throw new IllegalStateException("Profile already exists with mobileNo " + profileRequest.getMobileno());
        }
        boolean usernameTaken = profilesMap.values().stream()
                .anyMatch(p -> p.getUsername().equals(profileRequest.getUsername()));
        if (usernameTaken){
            throw new IllegalStateException("Profile already exists with username " + profileRequest.getUsername());
        }
        Profile profile=new Profile(profileRequest.getUsername(),
                profileRequest.getName(),profileRequest.getDob(),
                profileRequest.getMobileno());
        profilesMap.put(profile.getId(), profile);
        return profile;
    }
    public synchronized void deleteProfile(int  profileId){
        if (!profilesMap.containsKey(profileId)){
            throw new IllegalStateException("Profile does not exist with profile id " + profileId);
        }
        profilesMap.remove(profileId);
    }
    public synchronized Profile getProfile(int  profileId){
        if (!profilesMap.containsKey(profileId)){
            throw new IllegalStateException("Profile does not exist with profile id " + profileId);
        }
        return profilesMap.get(profileId);
    }
}
