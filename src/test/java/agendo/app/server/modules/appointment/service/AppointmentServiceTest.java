package agendo.app.server.modules.appointment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import agendo.app.server.modules.user.models.UserRole;

/**
 * Testes unitários do AppointmentService — cobre o cálculo do valor total
 * do agendamento e a máquina de estados de transição de status
 * (PENDING -> APPROVED/REJECTED -> CANCELLED/COMPLETED), incluindo as
 * regras de quem pode fazer cada transição.
 *
 * Nenhuma dependência real é usada: todos os repositórios são mockados
 * com Mockito, então este teste roda em milissegundos e não toca o banco.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentServiceRepository appointmentServiceRepository;

    @Mock
    private AppointmentHistoryRepository appointmentHistoryRepository;

    private AppointmentService appointmentService;

    private UserEntity professional;
    private UserEntity client;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(
                appointmentRepository, appointmentServiceRepository, appointmentHistoryRepository);

        professional = UserEntity.builder().id(1L).name("Profissional Teste").role(UserRole.PROFESSIONAL).build();
        client = UserEntity.builder().id(2L).name("Cliente Teste").role(UserRole.CLIENT).build();
    }

    // teste para create()

    @Test
    void create_deveCalcularValorTotalComoSomaDosServicos() {
        ServiceTypeEntity service1 = ServiceTypeEntity.builder().id(10L).name("Corte").price(new BigDecimal("50.00")).build();
        ServiceTypeEntity service2 = ServiceTypeEntity.builder().id(11L).name("Barba").price(new BigDecimal("30.00")).build();

        AppointmentEntity appointment = AppointmentEntity.builder()
                .professional(professional)
                .client(client)
                .scheduleDate(LocalDateTime.now().plusDays(1))
                .build();

        when(appointmentRepository.save(any(AppointmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentEntity saved = appointmentService.create(appointment, List.of(service1, service2), client);

        assertThat(saved.getTotalAmount()).isEqualTo(new BigDecimal("80.00"));
    }

    @Test
    void create_devePersistirUmAppointmentServiceParaCadaServico() {
        ServiceTypeEntity service1 = ServiceTypeEntity.builder().id(10L).price(new BigDecimal("50.00")).build();
        ServiceTypeEntity service2 = ServiceTypeEntity.builder().id(11L).price(new BigDecimal("30.00")).build();

        AppointmentEntity appointment = AppointmentEntity.builder()
                .professional(professional)
                .client(client)
                .build();

        when(appointmentRepository.save(any(AppointmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        appointmentService.create(appointment, List.of(service1, service2), client);

        verify(appointmentServiceRepository, times(2)).save(any(AppointmentServiceEntity.class));
    }

    @Test
    void create_deveRegistrarHistoricoComStatusInicialPending() {
        AppointmentEntity appointment = AppointmentEntity.builder()
                .professional(professional)
                .client(client)
                .build();

        when(appointmentRepository.save(any(AppointmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        appointmentService.create(appointment, List.of(), client);

        ArgumentCaptor<AppointmentHistoryEntity> captor = ArgumentCaptor.forClass(AppointmentHistoryEntity.class);
        verify(appointmentHistoryRepository).save(captor.capture());

        AppointmentHistoryEntity history = captor.getValue();
        assertThat(history.getPreviousStatus()).isNull();
        assertThat(history.getNewStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(history.getChangedBy()).isEqualTo(client);
    }

    // updateStatus(): APPROVED

    @Test
    void updateStatus_profissionalPodeAprovarAgendamentoPendente() {
        AppointmentEntity appointment = AppointmentEntity.builder()
                .id(100L).professional(professional).client(client)
                .status(AppointmentStatus.PENDING)
                .build();

        when(appointmentRepository.findByIdAndParticipant(100L, professional)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentEntity result = appointmentService.updateStatus(100L, AppointmentStatus.APPROVED, professional);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.APPROVED);
    }

    @Test
    void updateStatus_clienteNaoPodeAprovarAgendamento() {
        AppointmentEntity appointment = AppointmentEntity.builder()
                .id(100L).professional(professional).client(client)
                .status(AppointmentStatus.PENDING)
                .build();

        when(appointmentRepository.findByIdAndParticipant(100L, client)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.updateStatus(100L, AppointmentStatus.APPROVED, client))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void updateStatus_naoPodeAprovarAgendamentoQueNaoEstaPending() {
        AppointmentEntity appointment = AppointmentEntity.builder()
                .id(100L).professional(professional).client(client)
                .status(AppointmentStatus.APPROVED) // já aprovado
                .build();

        when(appointmentRepository.findByIdAndParticipant(100L, professional)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.updateStatus(100L, AppointmentStatus.APPROVED, professional))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    // updateStatus(): REJECTED

    @Test
    void updateStatus_profissionalPodeRejeitarAgendamentoPendente() {
        AppointmentEntity appointment = AppointmentEntity.builder()
                .id(101L).professional(professional).client(client)
                .status(AppointmentStatus.PENDING)
                .build();

        when(appointmentRepository.findByIdAndParticipant(101L, professional)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentEntity result = appointmentService.updateStatus(101L, AppointmentStatus.REJECTED, professional);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.REJECTED);
    }

    @Test
    void updateStatus_clienteNaoPodeRejeitarAgendamento() {
        AppointmentEntity appointment = AppointmentEntity.builder()
                .id(101L).professional(professional).client(client)
                .status(AppointmentStatus.PENDING)
                .build();

        when(appointmentRepository.findByIdAndParticipant(101L, client)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.updateStatus(101L, AppointmentStatus.REJECTED, client))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // updateStatus(): CANCELLED

    @Test
    void updateStatus_clientePodeCancelarAgendamentoAprovado() {
        AppointmentEntity appointment = AppointmentEntity.builder()
                .id(102L).professional(professional).client(client)
                .status(AppointmentStatus.APPROVED)
                .build();

        when(appointmentRepository.findByIdAndParticipant(102L, client)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentEntity result = appointmentService.updateStatus(102L, AppointmentStatus.CANCELLED, client);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void updateStatus_naoPodeCancelarAgendamentoQueNaoFoiAprovado() {
        AppointmentEntity appointment = AppointmentEntity.builder()
                .id(102L).professional(professional).client(client)
                .status(AppointmentStatus.PENDING) // ainda não aprovado
                .build();

        when(appointmentRepository.findByIdAndParticipant(102L, client)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.updateStatus(102L, AppointmentStatus.CANCELLED, client))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    // updateStatus(): COMPLETED

    @Test
    void updateStatus_profissionalPodeConcluirAgendamentoAprovado() {
        AppointmentEntity appointment = AppointmentEntity.builder()
                .id(103L).professional(professional).client(client)
                .status(AppointmentStatus.APPROVED)
                .build();

        when(appointmentRepository.findByIdAndParticipant(103L, professional)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentEntity result = appointmentService.updateStatus(103L, AppointmentStatus.COMPLETED, professional);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void updateStatus_clienteNaoPodeMarcarComoConcluido() {
        AppointmentEntity appointment = AppointmentEntity.builder()
                .id(103L).professional(professional).client(client)
                .status(AppointmentStatus.APPROVED)
                .build();

        when(appointmentRepository.findByIdAndParticipant(103L, client)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.updateStatus(103L, AppointmentStatus.COMPLETED, client))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // findByRole()
    @Test
    void findByRole_comRoleInvalidaLancaBadRequest() {
        assertThatThrownBy(() -> appointmentService.findByRole(client, "gerente"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void findByIdAndParticipant_naoEncontradoLancaNotFound() {
        when(appointmentRepository.findByIdAndParticipant(999L, client)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.findByIdAndParticipant(999L, client))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}
