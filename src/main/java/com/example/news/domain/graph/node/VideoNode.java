package com.example.news.domain.graph.node;

import lombok.*;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Node("Video")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoNode {

    @Id
    @Property("video_id")
    private String youtubeVideoId;

    // PostgreSQL PK — 그래프 노드 클릭 후 /api/v1/analysis/{targetId} 이동에 사용
    @Property("target_id")
    private Long targetId;

    private String title;

    private String description;

    @Property("country_code")
    private String countryCode;

    @Property("language")
    private String languageCode;

    @Property("channel_id")
    private String channelId;

    @Property("published_at")
    private LocalDateTime publishedAt;

    @Property("view_count")
    private Long viewCount;

    @Property("thumbnail_url")
    private String thumbnailUrl;

    @Property("video_keywords")
    @Builder.Default
    private List<String> videoKeywords = new ArrayList<>();

    @Relationship(type = "PUBLISHED_BY", direction = Relationship.Direction.OUTGOING)
    private ChannelNode channel;

    // load-then-update 패턴으로 기존 PART_OF 관계를 항상 보존
    @Builder.Default
    @Relationship(type = "PART_OF", direction = Relationship.Direction.OUTGOING)
    private List<IssueNode> issues = new ArrayList<>();

    public void addIssue(IssueNode issueNode) {
        if (issues == null) {
            issues = new ArrayList<>();
        }
        boolean alreadyLinked = issues.stream()
                .anyMatch(i -> i.getClusterId().equals(issueNode.getClusterId()));
        if (!alreadyLinked) {
            issues.add(issueNode);
        }
    }
}
