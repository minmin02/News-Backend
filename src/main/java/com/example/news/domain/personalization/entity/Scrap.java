package com.example.news.domain.personalization.entity;

import com.example.news.domain.personalization.enums.ScrapTargetType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "scrap")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scrap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id2", nullable = false)
    private Long userId;

    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ScrapTargetType targetType;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
