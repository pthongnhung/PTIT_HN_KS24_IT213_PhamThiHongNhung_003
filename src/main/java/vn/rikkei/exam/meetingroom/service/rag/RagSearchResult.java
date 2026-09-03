package vn.rikkei.exam.meetingroom.service.rag;

import vn.rikkei.exam.meetingroom.dto.SourceReference;

import java.util.List;

public record RagSearchResult(
        String context,
        List<SourceReference> sources
) {
    public boolean hasEvidence() {
        return sources != null && !sources.isEmpty();
    }
}
