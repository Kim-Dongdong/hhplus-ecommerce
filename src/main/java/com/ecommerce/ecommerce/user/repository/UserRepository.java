package com.ecommerce.ecommerce.user.repository;

import com.ecommerce.ecommerce.user.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findAllByName(String name);

    boolean existsByEmail(String email);
}
