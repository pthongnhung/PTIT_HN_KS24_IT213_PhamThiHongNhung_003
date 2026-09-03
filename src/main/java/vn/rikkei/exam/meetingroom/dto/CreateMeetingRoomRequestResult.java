package vn.rikkei.exam.meetingroom.dto;

public record CreateMeetingRoomRequestResult(
        String requestId,
        String status,
        String summary
) { }
