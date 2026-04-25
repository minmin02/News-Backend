package com.example.news.domain.user.entity;

import com.example.news.domain.user.enums.UserStatus;
import com.example.news.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = true)
    private String password;

    @Column(name = "name")
    private String name;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "birth")
    private LocalDate birth;

    @Column(name = "phone")
    private String phone;

    @Column(name = "profile_image_key")
    private String profileImageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    public void updateProfile(String name, String nickname, LocalDate birth, String phone) {
        if (name != null) this.name = name;
        if (nickname != null) this.nickname = nickname;
        if (birth != null) this.birth = birth;
        if (phone != null) this.phone = phone;
    }

    public void withdraw() {
        this.status = UserStatus.DELETED;
    }
}