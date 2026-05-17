package com.example.news.domain.graph.repository;

import com.example.news.domain.graph.node.VideoNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Optional;

public interface VideoNodeRepository extends Neo4jRepository<VideoNode, String> {
    Optional<VideoNode> findByTargetId(Long targetId);

    @Query("MATCH (v:Video {video_id: $videoId}) RETURN v")
    Optional<VideoNode> findNodeOnlyByVideoId(String videoId);
}
