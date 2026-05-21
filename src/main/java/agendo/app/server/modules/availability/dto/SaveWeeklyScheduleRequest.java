package agendo.app.server.modules.availability.dto;

import java.util.List;

public record SaveWeeklyScheduleRequest(List<DayScheduleRequest> schedule) {}
