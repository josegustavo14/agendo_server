package agendo.app.server.modules.appointment.models;

public enum AppointmentStatus {
    PENDING,    // client created, waiting for professional approval
    APPROVED,   // professional approved
    PAID,       // payment confirmed via AbacatePay webhook (billing.paid)
    REJECTED,   // professional rejected
    CANCELLED,  // cancelled by client or professional after approval
    COMPLETED   // appointment was attended
}
