package com.example.news.domain.graph.node;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("AnalysisResult")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResultNode {

    @Id
    @Property("analysis_result_id")
    private Long analysisResultId;

    @Property("opinion_score")
    private Double opinionScore;

    @Property("overall_bias_score")
    private Double overallBiasScore;

    private String status;
}
