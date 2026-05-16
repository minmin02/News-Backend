package com.example.news.global.event;

import java.util.List;

public record VideoSearchedEvent(
        String keyword,
        List<Long> videoDbIds
) {}
