package agendo.app.server.modules.appointment.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import agendo.app.server.modules.appointment.models.AppointmentEntity;
import agendo.app.server.modules.appointment.models.AppointmentHistoryEntity;
import agendo.app.server.modules.appointment.models.AppointmentServiceEntity;
import agendo.app.server.modules.appointment.models.AppointmentStatus;
import agendo.app.server.modules.appointment.models.ServiceTypeEntity;
import agendo.app.server.modules.appointment.repository.AppointmentHistoryRepository;
import agendo.app.server.modules.appointment.repository.AppointmentRepository;
import agendo.app.server.modules.appointment.repository.AppointmentServiceRepository;
import agendo.app.server.modules.user.models.UserEntity;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceRepository appointmentServiceRepository;
    private final AppointmentHistoryRepository appointmentHistoryRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public AppointmentEntity create(AppointmentEntity appointment, List<ServiceTypeEntity> serviceTypes, UserEntity createdBy) {
        BigDecimal total = serviceTypes.stream()
                .map(ServiceTypeEntity::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        appointment.setTotalAmount(total);
        AppointmentEntity saved = appointmentRepository.save(appointment);

        serviceTypes.forEach(st -> appointmentServiceRepository.save(
                AppointmentServiceEntity.builder()
                        .appointment(saved)
                        .serviceType(st)
                        .build()
        ));

        appointmentHistoryRepository.save(AppointmentHistoryEntity.builder()
                .appointment(saved)
                .previousStatus(null)
                .newStatus(AppointmentStatus.PENDING)
                .changedBy(createdBy)
                .build());

        return saved;
    }

    public AppointmentEntity findByIdAndParticipant(Long id, UserEntity user) {
        return appointmentRepository.findByIdAndParticipant(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found: " + id));
    }

    public List<AppointmentEntity> findByParticipant(UserEntity user) {
        return appointmentRepository.findByParticipant(user);
    }

    public List<AppointmentEntity> findByRole(UserEntity user, String role) {
        return switch (role) {
            case "professional" -> appointmentRepository.findByProfessional(user);
            case "client" -> appointmentRepository.findByClient(user);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role inválida. Use 'professional' ou 'client'");
        };
    }

    public List<AppointmentEntity> findActive(UserEntity user) {
        return appointmentRepository.findByParticipantAndStatuses(
                user, List.of(AppointmentStatus.PENDING, AppointmentStatus.APPROVED));
    }

    public List<AppointmentEntity> findArchive(UserEntity user) {
        return appointmentRepository.findByParticipantAndStatuses(
                user, List.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED, AppointmentStatus.REJECTED));
    }

    public List<AppointmentHistoryEntity> findTimeline(Long appointmentId, UserEntity user) {
        findByIdAndParticipant(appointmentId, user);
        return appointmentHistoryRepository.findByAppointmentId(appointmentId);
    }

    @Transactional
    public AppointmentEntity updateStatus(Long id, AppointmentStatus newStatus, UserEntity user) {
        AppointmentEntity appointment = findByIdAndParticipant(id, user);
        AppointmentStatus currentStatus = appointment.getStatus();
        boolean isProfessional = appointment.getProfessional().getId().equals(user.getId());

        switch (newStatus) {
            case APPROVED -> {
                if (!isProfessional) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only professional can approve");
                }
                if (currentStatus != AppointmentStatus.PENDING) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only approve PENDING appointments");
                }
            }
            case REJECTED -> {
                if (!isProfessional) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only professional can reject");
                }
                if (currentStatus != AppointmentStatus.PENDING) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only reject PENDING appointments");
                }
            }
            case CANCELLED -> {
                if (currentStatus != AppointmentStatus.APPROVED) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only cancel APPROVED appointments");
                }
            }
            case COMPLETED -> {
                if (!isProfessional) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only professional can mark as completed");
                }
                if (currentStatus != AppointmentStatus.APPROVED) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only complete APPROVED appointments");
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status transition");
        }

        appointment.setStatus(newStatus);
        AppointmentEntity saved = appointmentRepository.save(appointment);

        appointmentHistoryRepository.save(AppointmentHistoryEntity.builder()
                .appointment(saved)
                .previousStatus(currentStatus)
                .newStatus(newStatus)
                .changedBy(user)
                .build());

        // Ao aprovar, dispara (após o commit) a geração da cobrança PIX.
        // A publicação aqui só registra o evento; o listener AFTER_COMMIT
        // garante que a cobrança só é criada se esta transação confirmar.
        if (newStatus == AppointmentStatus.APPROVED) {
            eventPublisher.publishEvent(
                    new agendo.app.server.modules.appointment.events.AppointmentApprovedEvent(saved.getId()));
        }

        return saved;
    }
}