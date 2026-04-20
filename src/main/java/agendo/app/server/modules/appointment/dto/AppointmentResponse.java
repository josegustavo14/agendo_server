package agendo.app.server.modules.appointment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import agendo.app.server.modules.appointment.models.AppointmentStatus;

public record AppointmentResponse(
    Long id,
    UserSummary professional,
    UserSummary client,
    List<ServiceTypeSummary> services,
    BigDecimal totalAmount,
    LocalDateTime scheduleDate,
    LocalDateTime requestDate,
    AppointmentStatus status
) {
    public record UserSummary(Long id, String name) {}
    public record ServiceTypeSummary(Long id, String name, BigDecimal price) {}
}
