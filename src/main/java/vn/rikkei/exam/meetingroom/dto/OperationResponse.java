package vn.rikkei.exam.meetingroom.dto;

public record OperationResponse(
        String requestId,
        String status,
        String message
) { }
