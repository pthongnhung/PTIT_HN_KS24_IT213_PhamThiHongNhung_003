package vn.rikkei.exam.meetingroom.dto;

import java.util.List;

public record AssistantAskResponse(
        String answer,
        String conversationId,
        List<SourceReference> sources,
        List<String> toolsUsed
) { }
