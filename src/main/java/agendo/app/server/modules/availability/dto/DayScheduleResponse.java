package agendo.app.server.modules.availability.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DayScheduleResponse(
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    int slotDurationMinutes
) {}
