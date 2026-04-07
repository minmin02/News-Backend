package com.example.news.domain.user.repository;

import com.example.news.domain.user.entity.UserSocialAccount;
import com.example.news.domain.user.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

    Optional<UserSocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
