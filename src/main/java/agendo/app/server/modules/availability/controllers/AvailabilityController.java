package agendo.app.server.modules.availability.controllers;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import agendo.app.server.modules.availability.dto.DayScheduleResponse;
import agendo.app.server.modules.availability.dto.SaveWeeklyScheduleRequest;
import agendo.app.server.modules.availability.dto.TimeSlotResponse;
import agendo.app.server.modules.availability.service.AvailabilityService;
import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/availability")
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Gerenciamento de horários disponíveis")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final UserService userService;

    @GetMapping("/schedule")
    @Operation(summary = "Retorna a grade semanal do profissional autenticado")
    public ResponseEntity<List<DayScheduleResponse>> getSchedule(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(availabilityService.getSchedule(user));
    }

    @PostMapping("/schedule")
    @Operation(summary = "Salva/atualiza a grade semanal do profissional autenticado")
    public ResponseEntity<List<DayScheduleResponse>> saveSchedule(
            @RequestBody SaveWeeklyScheduleRequest request,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(availabilityService.saveSchedule(user, request.schedule()));
    }

    @DeleteMapping("/schedule/{dayOfWeek}")
    @Operation(summary = "Remove um dia da grade semanal do profissional autenticado")
    public ResponseEntity<Void> deleteDay(
            @PathVariable DayOfWeek dayOfWeek,
            @AuthenticationPrincipal UserEntity user) {
        availabilityService.deleteDay(user, dayOfWeek);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{professionalId}/slots")
    @Operation(summary = "Retorna os slots disponíveis de um profissional em uma data específica")
    public ResponseEntity<List<TimeSlotResponse>> getSlots(
            @PathVariable Long professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UserEntity professional = userService.findById(professionalId);
        return ResponseEntity.ok(availabilityService.getSlots(professional, date));
    }
}
