package agendo.app.server.modules.payment.repository;


import agendo.app.server.modules.payment.models.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByAbacatePayBillingId(String billingId);

    Optional<PaymentEntity> findByAppointmentId(Long appointmentId);
}

