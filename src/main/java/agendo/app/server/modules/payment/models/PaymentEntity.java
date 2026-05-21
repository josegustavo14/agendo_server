package agendo.app.server.modules.payment.models;

import agendo.app.server.modules.appointment.models.AppointmentEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * registra cobranças geradas via AbacatePay, associadas a agendamentos.
 */
@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID da cobrança retornado pela AbacatePay (ex: "bill_abc123"). */
    @Column(name = "abacatepay_billing_id", nullable = false, unique = true)
    private String abacatePayBillingId;

    /** link de pagamento gerado pela AbacatePay. */
    @Column(name = "payment_url", nullable = false)
    private String paymentUrl;

    /** status atual: PENDING | PAID | EXPIRED | CANCELLED | REFUNDED | FAILED */
    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    /** valor em centavos. */
    @Column(nullable = false)
    private Integer amountInCents;

    /** agendamento que originou esta cobrança. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private AppointmentEntity appointment;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

