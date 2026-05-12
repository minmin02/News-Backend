package com.example.news.domain.user.repository;

import com.example.news.domain.user.entity.User;
import com.example.news.domain.user.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
