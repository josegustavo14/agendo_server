package agendo.app.server.modules.appointment.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import agendo.app.server.modules.appointment.dto.AppointmentHistoryResponse;
import agendo.app.server.modules.appointment.dto.AppointmentResponse;
import agendo.app.server.modules.appointment.dto.AppointmentResponse.ServiceTypeSummary;
import agendo.app.server.modules.appointment.dto.AppointmentResponse.UserSummary;
import agendo.app.server.modules.appointment.dto.CreateAppointmentRequest;
import agendo.app.server.modules.appointment.models.AppointmentHistoryEntity;
import agendo.app.server.modules.appointment.models.AppointmentEntity;
import agendo.app.server.modules.appointment.models.AppointmentServiceEntity;
import agendo.app.server.modules.appointment.models.AppointmentStatus;
import agendo.app.server.modules.appointment.models.ServiceTypeEntity;
import agendo.app.server.modules.appointment.repository.AppointmentServiceRepository;
import agendo.app.server.modules.appointment.service.AppointmentService;
import agendo.app.server.modules.appointment.service.ServiceTypeService;
import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Gerenciamento de agendamentos")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentServiceRepository appointmentServiceRepository;
    private final ServiceTypeService serviceTypeService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "Criar agendamento", description = "O usuário autenticado deve ser o profissional ou o cliente. Suporta múltiplos serviços — o totalAmount é calculado automaticamente.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Agendamento criado com sucesso"),
        @ApiResponse(responseCode = "403", description = "Usuário autenticado não é parte deste agendamento"),
        @ApiResponse(responseCode = "404", description = "Profissional, cliente ou tipo de serviço não encontrado")
    })
    public ResponseEntity<AppointmentResponse> create(
            @RequestBody CreateAppointmentRequest request,
            @AuthenticationPrincipal UserEntity user) {

        boolean isProfessional = user.getId().equals(request.professionalId());
        boolean isClient = user.getId().equals(request.clientId());
        if (!isProfessional && !isClient) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você deve ser o profissional ou o cliente do agendamento");
        }

        UserEntity professional = isProfessional ? user : userService.findById(request.professionalId());
        UserEntity client = isClient ? user : userService.findById(request.clientId());

        List<ServiceTypeEntity> serviceTypes = request.serviceTypeIds().stream()
                .map(id -> serviceTypeService.findByIdAndOwner(id, professional))
                .toList();

        AppointmentEntity appointment = AppointmentEntity.builder()
                .professional(professional)
                .client(client)
                .scheduleDate(request.scheduleDate())
                .build();

        return ResponseEntity.status(201).body(toResponse(appointmentService.create(appointment, serviceTypes, user)));
    }

    @GetMapping
    @Operation(
        summary = "Listar agendamentos",
        description = "Retorna agendamentos do usuário. Use 'role' para filtrar por 'professional' ou 'client'."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Role inválida")
    })
    public ResponseEntity<List<AppointmentResponse>> findAll(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(required = false) String role) {
        List<AppointmentEntity> appointments = role != null
                ? appointmentService.findByRole(user, role)
                : appointmentService.findByParticipant(user);
        return ResponseEntity.ok(appointments.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar agendamento por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Agendamento encontrado"),
        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado ou sem permissão")
    })
    public ResponseEntity<AppointmentResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(toResponse(appointmentService.findByIdAndParticipant(id, user)));
    }

    @GetMapping("/professional")
    @Operation(summary = "Listar agendamentos como profissional", description = "Retorna apenas agendamentos onde o usuário autenticado é o profissional.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
    })
    public ResponseEntity<List<AppointmentResponse>> findProfessional(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(
            appointmentService.findByRole(user, "professional").stream().map(this::toResponse).toList()
        );
    }   

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Aprovar agendamento", description = "Apenas o profissional pode aprovar")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Agendamento aprovado"),
        @ApiResponse(responseCode = "403", description = "Permissão negada"),
        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado"),
        @ApiResponse(responseCode = "400", description = "Agendamento não está em status PENDING")
    })
    public ResponseEntity<AppointmentResponse> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(toResponse(appointmentService.updateStatus(id, AppointmentStatus.APPROVED, user)));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Rejeitar agendamento", description = "Apenas o profissional pode rejeitar")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Agendamento rejeitado"),
        @ApiResponse(responseCode = "403", description = "Permissão negada"),
        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado"),
        @ApiResponse(responseCode = "400", description = "Agendamento não está em status PENDING")
    })
    public ResponseEntity<AppointmentResponse> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(toResponse(appointmentService.updateStatus(id, AppointmentStatus.REJECTED, user)));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar agendamento", description = "Cliente ou profissional pode cancelar após aprovação")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Agendamento cancelado"),
        @ApiResponse(responseCode = "403", description = "Permissão negada"),
        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado"),
        @ApiResponse(responseCode = "400", description = "Agendamento não está em status APPROVED")
    })
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(toResponse(appointmentService.updateStatus(id, AppointmentStatus.CANCELLED, user)));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Marcar como concluído", description = "Apenas o profissional pode marcar como concluído")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Agendamento marcado como concluído"),
        @ApiResponse(responseCode = "403", description = "Permissão negada"),
        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado"),
        @ApiResponse(responseCode = "400", description = "Agendamento não está em status APPROVED")
    })
    public ResponseEntity<AppointmentResponse> complete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(toResponse(appointmentService.updateStatus(id, AppointmentStatus.COMPLETED, user)));
    }

    @GetMapping("/active")
    @Operation(
        summary = "Agendamentos ativos",
        description = "Retorna agendamentos com status PENDING ou APPROVED do usuário autenticado. Ideal para a home do app."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<AppointmentResponse>> findActive(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(
            appointmentService.findActive(user).stream().map(this::toResponse).toList()
        );
    }

    @GetMapping("/archive")
    @Operation(
        summary = "Histórico de agendamentos",
        description = "Retorna agendamentos encerrados (COMPLETED, CANCELLED, REJECTED) do usuário autenticado."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<AppointmentResponse>> findArchive(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(
            appointmentService.findArchive(user).stream().map(this::toResponse).toList()
        );
    }

    @GetMapping("/{id}/timeline")
    @Operation(
        summary = "Timeline de um agendamento",
        description = "Retorna a trilha cronológica de mudanças de status de um agendamento. Apenas participantes têm acesso."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Timeline retornada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Agendamento não encontrado ou sem permissão")
    })
    public ResponseEntity<List<AppointmentHistoryResponse>> findTimeline(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity user) {
        List<AppointmentHistoryEntity> history = appointmentService.findTimeline(id, user);
        List<AppointmentHistoryResponse> response = history.stream()
                .map(h -> new AppointmentHistoryResponse(
                        h.getPreviousStatus(),
                        h.getNewStatus(),
                        new AppointmentHistoryResponse.UserSummary(h.getChangedBy().getId(), h.getChangedBy().getName()),
                        h.getChangedAt()))
                .toList();
        return ResponseEntity.ok(response);
    }

    private AppointmentResponse toResponse(AppointmentEntity a) {
        List<AppointmentServiceEntity> services = appointmentServiceRepository.findByAppointmentId(a.getId());

        List<ServiceTypeSummary> serviceSummaries = services.stream()
                .map(s -> new ServiceTypeSummary(
                        s.getServiceType().getId(),
                        s.getServiceType().getName(),
                        s.getServiceType().getPrice()))
                .toList();

        return new AppointmentResponse(
            a.getId(),
            new UserSummary(a.getProfessional().getId(), a.getProfessional().getName()),
            new UserSummary(a.getClient().getId(), a.getClient().getName()),
            serviceSummaries,
            a.getTotalAmount(),
            a.getScheduleDate(),
            a.getRequestDate(),
            a.getStatus()
        );
    }
}
