package com.example.news.domain.chatbot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_session")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title")
    private String title;

    @Column(name = "pending_context_type")
    private String pendingContextType;

    @Column(name = "pending_context_json", columnDefinition = "TEXT")
    private String pendingContextJson;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updatePendingContext(String pendingContextType, String pendingContextJson) {
        this.pendingContextType = pendingContextType;
        this.pendingContextJson = pendingContextJson;
    }

    public void clearPendingContext() {
        this.pendingContextType = null;
        this.pendingContextJson = null;
    }
}
