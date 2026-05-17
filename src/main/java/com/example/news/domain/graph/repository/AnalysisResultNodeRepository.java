package com.example.news.domain.graph.repository;

import com.example.news.domain.graph.node.AnalysisResultNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface AnalysisResultNodeRepository extends Neo4jRepository<AnalysisResultNode, Long> {
}
