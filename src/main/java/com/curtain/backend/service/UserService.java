package com.curtain.backend.service;

import com.curtain.backend.entity.User;
import com.curtain.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }
    
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id).orElseThrow();
        user.setUsername(userDetails.getUsername());
        user.setPassword(userDetails.getPassword());
        user.setRole(userDetails.getRole());
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public User login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            System.out.println("User found: " + user.get().getUsername() + ", Role: " + user.get().getRole());
            System.out.println("Comparing passwords: input[" + password + "] with DB[" + user.get().getPassword() + "]");
            if (user.get().getPassword().equals(password)) {
                return user.get();
            }
        }
        return null;
    }
}
