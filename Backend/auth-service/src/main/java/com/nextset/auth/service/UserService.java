package com.nextset.auth.service;

import com.nextset.auth.model.User;
import com.nextset.auth.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Looks up a user by their Google ID. If none exists yet, creates one.
     * If the user exists but their name/picture changed on Google's side,
     * updates those fields to stay in sync.
     */
    public User findOrCreateUser(String googleId, String email, String name, String pictureUrl) {
        return userRepository.findByGoogleId(googleId)
                .map(existingUser -> {
                    boolean changed = false;

                    if (name != null && !name.equals(existingUser.getName())) {
                        existingUser.setName(name);
                        changed = true;
                    }
                    if (pictureUrl != null && !pictureUrl.equals(existingUser.getPictureUrl())) {
                        existingUser.setPictureUrl(pictureUrl);
                        changed = true;
                    }

                    return changed ? userRepository.save(existingUser) : existingUser;
                })
                .orElseGet(() -> {
                    User newUser = new User(email, name, pictureUrl, googleId);
                    return userRepository.save(newUser);
                });
    }
}