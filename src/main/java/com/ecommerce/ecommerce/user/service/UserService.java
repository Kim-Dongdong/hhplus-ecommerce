package com.ecommerce.ecommerce.user.service;

import com.ecommerce.ecommerce.user.domain.User;
import com.ecommerce.ecommerce.user.dto.UserRequest;
import com.ecommerce.ecommerce.user.dto.UserResponse;
import com.ecommerce.ecommerce.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), encodedPassword, request.name());
        User saved = userRepository.save(user);
        return UserResponse.toResponse(saved);
    }

    public UserResponse findById(Long id) {
        return UserResponse.toResponse(getUser(id));
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = getUser(id);
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }
        user.update(request.name(), request.email());
        return UserResponse.toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(getUser(id));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found. id=" + id));
    }
}
