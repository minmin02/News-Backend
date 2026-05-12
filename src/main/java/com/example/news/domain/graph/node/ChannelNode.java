package com.example.news.domain.graph.node;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("Channel")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelNode {

    @Id
    @Property("channel_id")
    private String channelId;

    @Property("channel_name")
    private String channelName;

    @Property("country_code")
    private String countryCode;
}
