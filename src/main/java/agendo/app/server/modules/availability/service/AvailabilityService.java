package agendo.app.server.modules.availability.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import agendo.app.server.modules.appointment.models.AppointmentEntity;
import agendo.app.server.modules.appointment.models.AppointmentStatus;
import agendo.app.server.modules.appointment.repository.AppointmentRepository;
import agendo.app.server.modules.availability.dto.DayScheduleRequest;
import agendo.app.server.modules.availability.dto.DayScheduleResponse;
import agendo.app.server.modules.availability.dto.TimeSlotResponse;
import agendo.app.server.modules.availability.models.WeeklyScheduleEntity;
import agendo.app.server.modules.availability.repository.WeeklyScheduleRepository;
import agendo.app.server.modules.user.models.UserEntity;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final AppointmentRepository appointmentRepository;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public List<DayScheduleResponse> getSchedule(UserEntity professional) {
        return weeklyScheduleRepository.findByProfessional(professional).stream()
                .map(e -> new DayScheduleResponse(e.getDayOfWeek(), e.getStartTime(), e.getEndTime(), e.getSlotDurationMinutes()))
                .toList();
    }

    @Transactional
    public List<DayScheduleResponse> saveSchedule(UserEntity professional, List<DayScheduleRequest> days) {
        for (DayScheduleRequest day : days) {
            WeeklyScheduleEntity entity = weeklyScheduleRepository
                    .findByProfessionalAndDayOfWeek(professional, day.dayOfWeek())
                    .orElse(WeeklyScheduleEntity.builder().professional(professional).build());

            entity.setDayOfWeek(day.dayOfWeek());
            entity.setStartTime(day.startTime());
            entity.setEndTime(day.endTime());
            entity.setSlotDurationMinutes(day.slotDurationMinutes() > 0 ? day.slotDurationMinutes() : 60);
            weeklyScheduleRepository.save(entity);
        }

        return getSchedule(professional);
    }

    @Transactional
    public void deleteDay(UserEntity professional, DayOfWeek dayOfWeek) {
        weeklyScheduleRepository.deleteByProfessionalAndDayOfWeek(professional, dayOfWeek);
    }

    public List<TimeSlotResponse> getSlots(UserEntity professional, LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();

        return weeklyScheduleRepository.findByProfessionalAndDayOfWeek(professional, dow)
                .map(schedule -> buildSlots(professional, date, schedule))
                .orElse(List.of());
    }

    private List<TimeSlotResponse> buildSlots(UserEntity professional, LocalDate date, WeeklyScheduleEntity schedule) {
        List<LocalDateTime> approvedTimes = appointmentRepository
                .findByProfessionalAndDateAndStatus(professional, date, AppointmentStatus.APPROVED)
                .stream()
                .map(AppointmentEntity::getScheduleDate)
                .toList();

        List<TimeSlotResponse> slots = new ArrayList<>();
        LocalTime cursor = schedule.getStartTime();
        int duration = schedule.getSlotDurationMinutes();

        while (!cursor.plusMinutes(duration).isAfter(schedule.getEndTime())) {
            LocalDateTime slotStart = date.atTime(cursor);
            LocalDateTime slotEnd = slotStart.plusMinutes(duration);

            boolean taken = approvedTimes.stream()
                    .anyMatch(t -> !t.isBefore(slotStart) && t.isBefore(slotEnd));

            slots.add(new TimeSlotResponse(cursor.format(TIME_FMT), !taken));
            cursor = cursor.plusMinutes(duration);
        }

        return slots;
    }
}
