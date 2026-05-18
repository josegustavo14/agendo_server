package agendo.app.server.modules.appointment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import agendo.app.server.modules.appointment.models.AppointmentHistoryEntity;

public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistoryEntity, Long> {

    @Query("SELECT h FROM AppointmentHistoryEntity h JOIN FETCH h.changedBy WHERE h.appointment.id = :appointmentId ORDER BY h.changedAt ASC")
    List<AppointmentHistoryEntity> findByAppointmentId(@Param("appointmentId") Long appointmentId);
}
