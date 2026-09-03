package vn.rikkei.exam.meetingroom.dto;

public record IngestResponse(
        String source,
        int totalChunks,
        int writtenChunks,
        int skippedChunks,
        String message
) { }
