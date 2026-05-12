package com.example.news.domain.graph.repository;

import com.example.news.domain.graph.node.VideoNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface VideoNodeRepository extends Neo4jRepository<VideoNode, String> {
}
