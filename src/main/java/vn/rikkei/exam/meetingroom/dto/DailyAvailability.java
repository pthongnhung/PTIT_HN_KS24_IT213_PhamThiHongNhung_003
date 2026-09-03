package vn.rikkei.exam.meetingroom.dto;

import java.time.LocalDate;

public record DailyAvailability(
        LocalDate date,
        int availableSlots,
        boolean available
) { }
