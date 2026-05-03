package com.example.news.domain.chatbot.repository;

import com.example.news.domain.chatbot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllBySessionIdOrderByCreatedAtAsc(Long sessionId);

    Optional<ChatMessage> findTopBySessionIdOrderByCreatedAtDesc(Long sessionId);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.sessionId = :sessionId")
    void deleteAllBySessionId(@Param("sessionId") Long sessionId);
}
