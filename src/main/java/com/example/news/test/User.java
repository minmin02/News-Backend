package com.example.news.test;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 테스트 엔티티
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;


}