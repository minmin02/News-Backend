package com.example.news.domain.graph.repository;

import com.example.news.domain.graph.node.ChannelNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ChannelNodeRepository extends Neo4jRepository<ChannelNode, String> {
}
