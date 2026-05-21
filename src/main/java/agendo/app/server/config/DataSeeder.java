package agendo.app.server.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import agendo.app.server.modules.payment.repository.PaymentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import agendo.app.server.modules.appointment.models.AppointmentEntity;
import agendo.app.server.modules.appointment.models.AppointmentServiceEntity;
import agendo.app.server.modules.appointment.models.ServiceTypeEntity;
import agendo.app.server.modules.appointment.repository.AppointmentHistoryRepository;
import agendo.app.server.modules.appointment.repository.AppointmentRepository;
import agendo.app.server.modules.appointment.repository.AppointmentServiceRepository;
import agendo.app.server.modules.appointment.repository.ServiceTypeRepository;
import agendo.app.server.modules.rating.repository.RatingRepository;
import agendo.app.server.modules.user.models.ClientProfileEntity;
import agendo.app.server.modules.user.models.ProfessionalProfileEntity;
import agendo.app.server.modules.user.models.ProfessionEntity;
import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.modules.user.models.UserRole;
import agendo.app.server.modules.user.repository.ClientProfileRepository;
import agendo.app.server.modules.user.repository.ProfessionalProfileRepository;
import agendo.app.server.modules.user.repository.ProfessionRepository;
import agendo.app.server.modules.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// @Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ProfessionRepository professionRepository;
    private final ProfessionalProfileRepository professionalProfileRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final AppointmentHistoryRepository appointmentHistoryRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceRepository appointmentServiceRepository;
    private final RatingRepository ratingRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== Iniciando seed do banco de dados ===");

        // 1. Limpar tudo
        log.info("Excluindo todos os dados...");
        paymentRepository.deleteAllInBatch();
        appointmentHistoryRepository.deleteAllInBatch();
        appointmentServiceRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        ratingRepository.deleteAllInBatch();
        serviceTypeRepository.deleteAllInBatch();
        professionalProfileRepository.deleteAllInBatch();
        clientProfileRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        professionRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();
        log.info("Dados excluídos com sucesso.");

        // 2. Criar profissões
        ProfessionEntity eletricista = professionRepository.save(ProfessionEntity.builder().name("Eletricista").build());
        ProfessionEntity desenvolvedor = professionRepository.save(ProfessionEntity.builder().name("Desenvolvedor").build());
        ProfessionEntity encanador = professionRepository.save(ProfessionEntity.builder().name("Encanador").build());
        ProfessionEntity designer = professionRepository.save(ProfessionEntity.builder().name("Designer").build());
        ProfessionEntity personal = professionRepository.save(ProfessionEntity.builder().name("Personal Trainer").build());
        log.info("Profissões criadas: Eletricista, Desenvolvedor, Encanador, Designer, Personal Trainer");

        // 3. Criar usuários profissionais
        String senhaHash = passwordEncoder.encode("123456");

        UserEntity joao = userRepository.save(UserEntity.builder()
                .name("João Silva")
                .email("joao@email.com")
                .phone("11999990001")
                .role(UserRole.PROFESSIONAL)
                .passwordHash(senhaHash)
                .build());

        UserEntity maria = userRepository.save(UserEntity.builder()
                .name("Maria Oliveira")
                .email("maria@email.com")
                .phone("11999990002")
                .role(UserRole.PROFESSIONAL)
                .passwordHash(senhaHash)
                .build());

        UserEntity carlos = userRepository.save(UserEntity.builder()
                .name("Carlos Santos")
                .email("carlos@email.com")
                .phone("11999990003")
                .role(UserRole.PROFESSIONAL)
                .passwordHash(senhaHash)
                .build());

        // 4. Criar perfis profissionais
        professionalProfileRepository.save(ProfessionalProfileEntity.builder()
                .user(joao)
                .profession(eletricista)
                .bio("Eletricista com 10 anos de experiência em instalações residenciais e comerciais.")
                .isAvailable(true)
                .build());

        professionalProfileRepository.save(ProfessionalProfileEntity.builder()
                .user(maria)
                .profession(desenvolvedor)
                .bio("Desenvolvedora fullstack especializada em Java e React.")
                .isAvailable(true)
                .build());

        professionalProfileRepository.save(ProfessionalProfileEntity.builder()
                .user(carlos)
                .profession(encanador)
                .bio("Encanador profissional, atendo urgências 24h.")
                .isAvailable(false)
                .build());

        log.info("Profissionais criados: João (Eletricista), Maria (Desenvolvedora), Carlos (Encanador)");

        // 5. Criar usuários clientes
        UserEntity ana = userRepository.save(UserEntity.builder()
                .name("Ana Costa")
                .email("ana@email.com")
                .phone("11999990004")
                .role(UserRole.CLIENT)
                .passwordHash(senhaHash)
                .build());

        UserEntity pedro = userRepository.save(UserEntity.builder()
                .name("Pedro Almeida")
                .email("pedro@email.com")
                .phone("11999990005")
                .role(UserRole.CLIENT)
                .passwordHash(senhaHash)
                .build());

        // 6. Criar perfis de clientes
        clientProfileRepository.save(ClientProfileEntity.builder()
                .user(ana)
                .taxId("529.982.247-25")
                .preferredPaymentMethod("PIX")
                .build());

        clientProfileRepository.save(ClientProfileEntity.builder()
                .user(pedro)
                .taxId("529.982.247-25")
                .preferredPaymentMethod("CARTAO_CREDITO")
                .build());

        log.info("Clientes criados: Ana, Pedro");

        // 7. Criar tipos de serviço para João (Eletricista)
        ServiceTypeEntity instalacaoEletrica = serviceTypeRepository.save(ServiceTypeEntity.builder()
                .name("Instalação Elétrica")
                .description("Instalação de tomadas, disjuntores e fiação em geral")
                .price(new BigDecimal("150.00"))
                .owner(joao)
                .build());

        ServiceTypeEntity manutencaoEletrica = serviceTypeRepository.save(ServiceTypeEntity.builder()
                .name("Manutenção Elétrica")
                .description("Reparo e manutenção de sistemas elétricos")
                .price(new BigDecimal("120.00"))
                .owner(joao)
                .build());

        ServiceTypeEntity inspecaoEletrica = serviceTypeRepository.save(ServiceTypeEntity.builder()
                .name("Inspeção Elétrica")
                .description("Inspeção técnica de instalações elétricas")
                .price(new BigDecimal("80.00"))
                .owner(joao)
                .build());

        ServiceTypeEntity trocaDisjuntor = serviceTypeRepository.save(ServiceTypeEntity.builder()
                .name("Troca de Disjuntor")
                .description("Substituição de disjuntores e dispositivos de proteção")
                .price(new BigDecimal("100.00"))
                .owner(joao)
                .build());

        // 8. Criar tipos de serviço para Maria (Desenvolvedora)
        ServiceTypeEntity devWeb = serviceTypeRepository.save(ServiceTypeEntity.builder()
                .name("Desenvolvimento Web")
                .description("Criação de sites e aplicações web responsivas")
                .price(new BigDecimal("200.00"))
                .owner(maria)
                .build());

        ServiceTypeEntity devApi = serviceTypeRepository.save(ServiceTypeEntity.builder()
                .name("Desenvolvimento de API")
                .description("Criação de APIs REST e integração de sistemas")
                .price(new BigDecimal("180.00"))
                .owner(maria)
                .build());

        ServiceTypeEntity consultoriaWeb = serviceTypeRepository.save(ServiceTypeEntity.builder()
                .name("Consultoria Web")
                .description("Consultoria técnica para projetos web")
                .price(new BigDecimal("150.00"))
                .owner(maria)
                .build());

        ServiceTypeEntity debugFixBug = serviceTypeRepository.save(ServiceTypeEntity.builder()
                .name("Debug e Correção de Bugs")
                .description("Identificação e correção de erros em código existente")
                .price(new BigDecimal("100.00"))
                .owner(maria)
                .build());

        ServiceTypeEntity testingQA = serviceTypeRepository.save(ServiceTypeEntity.builder()
                .name("Testes e QA")
                .description("Testes automatizados e garantia de qualidade")
                .price(new BigDecimal("120.00"))
                .owner(maria)
                .build());

        // 9. Criar tipos de serviço para Carlos (Encanador)
        ServiceTypeEntity reparoHidraulico = serviceTypeRepository.save(ServiceTypeEntity.builder()
                .name("Reparo Hidráulico")
                .description("Conserto de vazamentos, troca de torneiras e válvulas")
                .price(new BigDecimal("130.00"))
                .owner(carlos)
                .build());

        ServiceTypeEntity instalacaoEncanacao = serviceTypeRepository.save(ServiceTypeEntity.builder()
                .name("Instalação de Encanação")
                .description("Instalação de tubulações e sistemas hidráulicos")
                .price(new BigDecimal("160.00"))
                .owner(carlos)
                .build());

        ServiceTypeEntity trocaSifao = serviceTypeRepository.save(ServiceTypeEntity.builder()
                .name("Troca de Sifão")
                .description("Substituição de sifões de pias, chuveiros e vasos")
                .price(new BigDecimal("90.00"))
                .owner(carlos)
                .build());

        ServiceTypeEntity limpezaTubulacao = serviceTypeRepository.save(ServiceTypeEntity.builder()
                .name("Limpeza de Tubulação")
                .description("Desobstrução e limpeza de canos entupidos")
                .price(new BigDecimal("110.00"))
                .owner(carlos)
                .build());

        log.info("Tipos de serviço criados: 13 serviços no total");

        // 10. Criar agendamentos com múltiplos serviços
        AppointmentEntity app1 = appointmentRepository.save(AppointmentEntity.builder()
                .professional(joao)
                .client(ana)
                .totalAmount(new BigDecimal("230.00"))
                .scheduleDate(LocalDateTime.of(2026, 3, 15, 9, 0))
                .build());

        appointmentServiceRepository.save(AppointmentServiceEntity.builder()
                .appointment(app1)
                .serviceType(instalacaoEletrica)
                .build());

        appointmentServiceRepository.save(AppointmentServiceEntity.builder()
                .appointment(app1)
                .serviceType(inspecaoEletrica)
                .build());

        AppointmentEntity app2 = appointmentRepository.save(AppointmentEntity.builder()
                .professional(maria)
                .client(pedro)
                .totalAmount(new BigDecimal("380.00"))
                .scheduleDate(LocalDateTime.of(2026, 3, 18, 14, 0))
                .build());

        appointmentServiceRepository.save(AppointmentServiceEntity.builder()
                .appointment(app2)
                .serviceType(devWeb)
                .build());

        appointmentServiceRepository.save(AppointmentServiceEntity.builder()
                .appointment(app2)
                .serviceType(debugFixBug)
                .build());

        AppointmentEntity app3 = appointmentRepository.save(AppointmentEntity.builder()
                .professional(joao)
                .client(pedro)
                .totalAmount(new BigDecimal("120.00"))
                .scheduleDate(LocalDateTime.of(2026, 3, 20, 10, 30))
                .build());

        appointmentServiceRepository.save(AppointmentServiceEntity.builder()
                .appointment(app3)
                .serviceType(manutencaoEletrica)
                .build());

        AppointmentEntity app4 = appointmentRepository.save(AppointmentEntity.builder()
                .professional(maria)
                .client(ana)
                .totalAmount(new BigDecimal("320.00"))
                .scheduleDate(LocalDateTime.of(2026, 3, 22, 16, 0))
                .build());

        appointmentServiceRepository.save(AppointmentServiceEntity.builder()
                .appointment(app4)
                .serviceType(devApi)
                .build());

        appointmentServiceRepository.save(AppointmentServiceEntity.builder()
                .appointment(app4)
                .serviceType(testingQA)
                .build());

        AppointmentEntity app5 = appointmentRepository.save(AppointmentEntity.builder()
                .professional(carlos)
                .client(ana)
                .totalAmount(new BigDecimal("240.00"))
                .scheduleDate(LocalDateTime.of(2026, 3, 25, 11, 0))
                .build());

        appointmentServiceRepository.save(AppointmentServiceEntity.builder()
                .appointment(app5)
                .serviceType(reparoHidraulico)
                .build());

        appointmentServiceRepository.save(AppointmentServiceEntity.builder()
                .appointment(app5)
                .serviceType(trocaSifao)
                .build());

        log.info("Agendamentos criados: 5 agendamentos com múltiplos serviços");
        log.info("=== Seed concluído com sucesso! ===");
        log.info("Senha padrão de todos os usuários: 123456");
    }
}
