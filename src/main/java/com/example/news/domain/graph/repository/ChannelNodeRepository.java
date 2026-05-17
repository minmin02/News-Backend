package com.example.news.domain.graph.repository;

import com.example.news.domain.graph.node.ChannelNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Optional;

public interface ChannelNodeRepository extends Neo4jRepository<ChannelNode, String> {
    @Query("MATCH (c:Channel {channel_id: $channelId}) RETURN c")
    Optional<ChannelNode> findNodeOnlyByChannelId(String channelId);
}
