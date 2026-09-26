package com.chubb.claims.repository;

import com.chubb.claims.domain.User;
import com.chubb.claims.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByRole(UserRole role);
}
