package agendo.app.server.modules.appointment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import agendo.app.server.modules.appointment.models.AppointmentEntity;
import agendo.app.server.modules.appointment.models.AppointmentStatus;
import agendo.app.server.modules.user.models.UserEntity;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long> {

    @Query("SELECT a FROM AppointmentEntity a JOIN FETCH a.professional JOIN FETCH a.client WHERE a.professional = :user OR a.client = :user")
    List<AppointmentEntity> findByParticipant(@Param("user") UserEntity user);

    @Query("SELECT a FROM AppointmentEntity a JOIN FETCH a.professional JOIN FETCH a.client WHERE a.professional = :user")
    List<AppointmentEntity> findByProfessional(@Param("user") UserEntity user);

    @Query("SELECT a FROM AppointmentEntity a JOIN FETCH a.professional JOIN FETCH a.client WHERE a.client = :user")
    List<AppointmentEntity> findByClient(@Param("user") UserEntity user);

    @Query("SELECT a FROM AppointmentEntity a JOIN FETCH a.professional JOIN FETCH a.client WHERE a.id = :id AND (a.professional = :user OR a.client = :user)")
    Optional<AppointmentEntity> findByIdAndParticipant(@Param("id") Long id, @Param("user") UserEntity user);

    @Query("SELECT a FROM AppointmentEntity a JOIN FETCH a.professional JOIN FETCH a.client WHERE (a.professional = :user OR a.client = :user) AND a.status IN :statuses ORDER BY a.scheduleDate ASC")
    List<AppointmentEntity> findByParticipantAndStatuses(@Param("user") UserEntity user, @Param("statuses") List<AppointmentStatus> statuses);
}
