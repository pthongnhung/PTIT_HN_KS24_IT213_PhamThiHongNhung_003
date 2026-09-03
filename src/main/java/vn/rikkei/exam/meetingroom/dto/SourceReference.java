package vn.rikkei.exam.meetingroom.dto;

public record SourceReference(
        String source,
        String section,
        String citation,
        Double score
) { }
