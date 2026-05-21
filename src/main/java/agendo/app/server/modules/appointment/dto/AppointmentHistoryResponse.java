package agendo.app.server.modules.appointment.dto;

import java.time.LocalDateTime;

import agendo.app.server.modules.appointment.models.AppointmentStatus;

public record AppointmentHistoryResponse(
    AppointmentStatus previousStatus,
    AppointmentStatus newStatus,
    UserSummary changedBy,
    LocalDateTime changedAt
) {
    public record UserSummary(Long id, String name) {}
}