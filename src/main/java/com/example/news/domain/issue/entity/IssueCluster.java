package com.example.news.domain.issue.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "issue_cluster")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String searchKeyword;

    private String clusterTitle;

    @Column(columnDefinition = "TEXT")
    private String clusterSummary;

    private LocalDate periodStartDate;

    private LocalDate periodEndDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
