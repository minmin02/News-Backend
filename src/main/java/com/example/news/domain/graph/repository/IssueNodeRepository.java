package com.example.news.domain.graph.repository;

import com.example.news.domain.graph.node.IssueNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Optional;

public interface IssueNodeRepository extends Neo4jRepository<IssueNode, Long> {
    @Query("MATCH (i:Issue {issue_id: $issueId}) RETURN i")
    Optional<IssueNode> findNodeOnlyByIssueId(Long issueId);
}
