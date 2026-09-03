package vn.rikkei.exam.meetingroom.dto;

import java.time.LocalDate;
import java.util.List;

public record MeetingRoomAvailabilityResult(
        String resourceType,
        LocalDate startDate,
        LocalDate endDate,
        boolean availableForWholeRange,
        int minimumAvailableSlots,
        List<DailyAvailability> dailyAvailability,
        String summary
) { }
