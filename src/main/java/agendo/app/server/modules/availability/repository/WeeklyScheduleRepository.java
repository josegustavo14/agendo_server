package agendo.app.server.modules.availability.repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import agendo.app.server.modules.availability.models.WeeklyScheduleEntity;
import agendo.app.server.modules.user.models.UserEntity;

public interface WeeklyScheduleRepository extends JpaRepository<WeeklyScheduleEntity, Long> {

    List<WeeklyScheduleEntity> findByProfessional(UserEntity professional);

    Optional<WeeklyScheduleEntity> findByProfessionalAndDayOfWeek(UserEntity professional, DayOfWeek dayOfWeek);

    void deleteByProfessionalAndDayOfWeek(UserEntity professional, DayOfWeek dayOfWeek);
}
