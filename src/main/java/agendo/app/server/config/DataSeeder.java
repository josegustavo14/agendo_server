package agendo.app.server.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;

import agendo.app.server.modules.appointment.models.AppointmentEntity;
import agendo.app.server.modules.appointment.models.AppointmentHistoryEntity;
import agendo.app.server.modules.appointment.models.AppointmentServiceEntity;
import agendo.app.server.modules.appointment.models.AppointmentStatus;
import agendo.app.server.modules.appointment.models.ServiceTypeEntity;
import agendo.app.server.modules.appointment.repository.AppointmentHistoryRepository;
import agendo.app.server.modules.appointment.repository.AppointmentRepository;
import agendo.app.server.modules.appointment.repository.AppointmentServiceRepository;
import agendo.app.server.modules.appointment.repository.ServiceTypeRepository;
import agendo.app.server.modules.availability.models.WeeklyScheduleEntity;
import agendo.app.server.modules.availability.repository.WeeklyScheduleRepository;
import agendo.app.server.modules.user.models.ClientProfileEntity;
import agendo.app.server.modules.user.models.ProfessionalProfileEntity;
import agendo.app.server.modules.user.models.ProfessionEntity;
import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.modules.user.models.UserRole;
import agendo.app.server.modules.rating.models.RatingEntity;
import agendo.app.server.modules.rating.repository.RatingRepository;
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
    private final ProfessionRepository professionRepository;
    private final ProfessionalProfileRepository professionalProfileRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceRepository appointmentServiceRepository;
    private final AppointmentHistoryRepository appointmentHistoryRepository;
    private final RatingRepository ratingRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Seed desabilitado (app.seed.enabled=false)");
            return;
        }

        if (userRepository.count() > 0) {
            log.info("Banco já possui dados, pulando seed.");
            return;
        }

        log.info("=== Iniciando seed ===");
        String hash = passwordEncoder.encode("123456");

        // --- Profissões ---
        var barbeiro = professionRepository.save(ProfessionEntity.builder().name("Barbeiro").build());
        var personal = professionRepository.save(ProfessionEntity.builder().name("Personal Trainer").build());
        var eletricista = professionRepository.save(ProfessionEntity.builder().name("Eletricista").build());

        // --- Profissionais ---
        var rafael = createProfessional("Rafael Barbosa", "rafael@email.com", "11999990001", hash, barbeiro,
                "Barbeiro há 8 anos, especialista em cortes modernos e barba.");
        var camila = createProfessional("Camila Ferreira", "camila@email.com", "11999990002", hash, personal,
                "Personal Trainer certificada CREF, foco em hipertrofia e emagrecimento.");
        var joao = createProfessional("João Eletricista", "joao.ele@email.com", "11999990003", hash, eletricista,
                "Eletricista residencial e comercial com 12 anos de experiência.");

        // --- Serviços Rafael (Barbeiro) ---
        var corteMasc   = createService(rafael, "Corte Masculino",      "Corte social ou moderno com máquina e tesoura", "45.00");
        var barba       = createService(rafael, "Barba Completa",       "Aparar, desenhar e navalha com toalha quente", "35.00");
        var corteBarba  = createService(rafael, "Corte + Barba",        "Combo corte masculino e barba completa", "70.00");
        var sobrancelha = createService(rafael, "Sobrancelha",          "Design de sobrancelha masculina com navalha", "20.00");
        var hidratacao  = createService(rafael, "Hidratação Capilar",   "Tratamento de hidratação profunda para cabelo", "50.00");
        var pigmentacao = createService(rafael, "Pigmentação de Barba", "Preenchimento de falhas na barba com pigmento", "80.00");

        // --- Serviços Camila (Personal) ---
        var avaliacao     = createService(camila, "Avaliação Física",        "Avaliação completa com bioimpedância e medidas corporais", "120.00");
        var treinoPersonal = createService(camila, "Treino Personalizado",   "Sessão individual de treino na academia (1h)", "90.00");
        var treinoFunc    = createService(camila, "Treino Funcional",        "Sessão de treino funcional ao ar livre (1h)", "80.00");
        var consultNutri  = createService(camila, "Consultoria Nutricional", "Orientação alimentar para objetivos fitness", "150.00");
        var planoMensal   = createService(camila, "Plano de Treino Mensal",  "Montagem de planilha personalizada para 30 dias", "200.00");
        var alongamento   = createService(camila, "Aula de Alongamento",     "Sessão de alongamento e mobilidade (45min)", "60.00");
        var treinoDupla   = createService(camila, "Treino em Dupla",         "Sessão de treino para 2 pessoas (1h)", "140.00");

        // --- Serviços João (Eletricista) ---
        var tomada     = createService(joao, "Instalação de Tomada",     "Instalação de tomada simples ou tripla", "80.00");
        var disjuntor  = createService(joao, "Troca de Disjuntor",       "Substituição de disjuntor no quadro elétrico", "100.00");
        var chuveiro   = createService(joao, "Instalação de Chuveiro",   "Instalação elétrica de chuveiro com fiação adequada", "120.00");
        var quadro     = createService(joao, "Manutenção de Quadro",     "Revisão e manutenção do quadro de distribuição", "180.00");
        var luminaria  = createService(joao, "Instalação de Luminária",  "Instalação de lustres, plafons e luminárias", "90.00");
        var fiacao     = createService(joao, "Passagem de Fiação",       "Passagem de cabos em eletrodutos existentes", "150.00");
        var inspecao   = createService(joao, "Inspeção Elétrica",        "Laudo técnico de instalações elétricas", "200.00");

        // --- Usuário de teste ---
        var teste = createClient("Usuário Teste", "teste@email.com", "11999990004", hash, "111.222.333-44", "PIX");

        // --- 10 usuários comuns ---
        var pessoas = new UserEntity[10];
        for (int i = 0; i < 10; i++) {
            pessoas[i] = createClient("Pessoa " + (i + 1), "pessoa" + (i + 1) + "@email.com",
                    "1199900100" + (i + 1), hash,
                    String.format("000.000.%03d-%02d", i + 1, i + 1), "PIX");
        }

        // --- Agendamentos do usuário teste ---

        // COMPLETED
        createAppointment(rafael, teste, AppointmentStatus.COMPLETED,
                dt(2026, 1, 10, 10, 0), dt(2026, 1, 7, 18, 0), corteBarba);

        createAppointment(camila, teste, AppointmentStatus.COMPLETED,
                dt(2026, 1, 20, 7, 0), dt(2026, 1, 15, 20, 0), avaliacao, treinoPersonal);

        createAppointment(joao, teste, AppointmentStatus.COMPLETED,
                dt(2026, 2, 5, 14, 0), dt(2026, 2, 1, 9, 0), tomada, luminaria);

        createAppointment(rafael, teste, AppointmentStatus.COMPLETED,
                dt(2026, 2, 14, 11, 0), dt(2026, 2, 10, 12, 0), corteMasc);

        createAppointment(camila, teste, AppointmentStatus.COMPLETED,
                dt(2026, 3, 3, 6, 30), dt(2026, 2, 28, 21, 0), treinoFunc, alongamento);

        // CANCELLED
        createAppointment(camila, teste, AppointmentStatus.CANCELLED,
                dt(2026, 3, 10, 8, 0), dt(2026, 3, 5, 19, 0), planoMensal);

        createAppointment(joao, teste, AppointmentStatus.CANCELLED,
                dt(2026, 3, 15, 9, 0), dt(2026, 3, 10, 10, 0), quadro);

        // REJECTED
        createAppointment(joao, teste, AppointmentStatus.REJECTED,
                dt(2026, 3, 20, 8, 0), dt(2026, 3, 16, 14, 0), fiacao);

        // APPROVED
        createAppointment(rafael, teste, AppointmentStatus.APPROVED,
                dt(2026, 4, 15, 10, 0), dt(2026, 4, 6, 22, 0), corteBarba, sobrancelha);

        createAppointment(camila, teste, AppointmentStatus.APPROVED,
                dt(2026, 4, 18, 7, 0), dt(2026, 4, 7, 20, 0), treinoPersonal);

        // PENDING
        createAppointment(joao, teste, AppointmentStatus.PENDING,
                dt(2026, 4, 25, 14, 0), dt(2026, 4, 9, 8, 0), inspecao, disjuntor);

        createAppointment(rafael, teste, AppointmentStatus.PENDING,
                dt(2026, 4, 30, 15, 0), dt(2026, 4, 9, 10, 0), pigmentacao);

        // --- Grade de horários ---
        // Rafael: Seg-Sex 09:00-19:00 (slots de 60min), Sáb 09:00-14:00 (slots de 30min)
        createSchedule(rafael, DayOfWeek.MONDAY,    "09:00", "19:00", 60);
        createSchedule(rafael, DayOfWeek.TUESDAY,   "09:00", "19:00", 60);
        createSchedule(rafael, DayOfWeek.WEDNESDAY, "09:00", "19:00", 60);
        createSchedule(rafael, DayOfWeek.THURSDAY,  "09:00", "19:00", 60);
        createSchedule(rafael, DayOfWeek.FRIDAY,    "09:00", "19:00", 60);
        createSchedule(rafael, DayOfWeek.SATURDAY,  "09:00", "14:00", 30);

        // Camila: Seg/Qua/Sex 06:00-12:00 e Ter/Qui 14:00-20:00 (slots de 60min)
        createSchedule(camila, DayOfWeek.MONDAY,    "06:00", "12:00", 60);
        createSchedule(camila, DayOfWeek.TUESDAY,   "14:00", "20:00", 60);
        createSchedule(camila, DayOfWeek.WEDNESDAY, "06:00", "12:00", 60);
        createSchedule(camila, DayOfWeek.THURSDAY,  "14:00", "20:00", 60);
        createSchedule(camila, DayOfWeek.FRIDAY,    "06:00", "12:00", 60);

        // João: Seg-Sex 08:00-18:00 (slots de 120min)
        createSchedule(joao, DayOfWeek.MONDAY,    "08:00", "18:00", 120);
        createSchedule(joao, DayOfWeek.TUESDAY,   "08:00", "18:00", 120);
        createSchedule(joao, DayOfWeek.WEDNESDAY, "08:00", "18:00", 120);
        createSchedule(joao, DayOfWeek.THURSDAY,  "08:00", "18:00", 120);
        createSchedule(joao, DayOfWeek.FRIDAY,    "08:00", "18:00", 120);

        // --- Avaliações das pessoas comuns para os profissionais ---
        // Rafael (Barbeiro) - 8 avaliações, média ~4.5
        createRating(rafael, pessoas[0], 5, "Melhor barbeiro da região, corte impecável!");
        createRating(rafael, pessoas[1], 5, "Muito atencioso, fez exatamente o que pedi.");
        createRating(rafael, pessoas[2], 4, "Bom corte, ambiente agradável.");
        createRating(rafael, pessoas[3], 5, "Barba perfeita, super recomendo!");
        createRating(rafael, pessoas[4], 4, "Profissional competente, voltarei com certeza.");
        createRating(rafael, pessoas[5], 5, "Atendimento excelente, pontual e caprichoso.");
        createRating(rafael, pessoas[6], 4, "Gostei do resultado, preço justo.");
        createRating(rafael, pessoas[7], 4, "Bom profissional, só demorou um pouco.");

        // Camila (Personal) - 7 avaliações, média ~4.7
        createRating(camila, pessoas[0], 5, "Treino muito bem montado, já estou vendo resultado!");
        createRating(camila, pessoas[2], 5, "Profissional dedicada, explica tudo com paciência.");
        createRating(camila, pessoas[3], 4, "Boa personal, treino puxado mas eficiente.");
        createRating(camila, pessoas[5], 5, "Camila é incrível, mudou minha rotina de treinos.");
        createRating(camila, pessoas[6], 5, "Avaliação física super completa e detalhada.");
        createRating(camila, pessoas[8], 5, "Melhor personal que já tive, vale cada centavo.");
        createRating(camila, pessoas[9], 4, "Muito boa, só achei a consultoria nutricional básica.");

        // João (Eletricista) - 6 avaliações, média ~4.2
        createRating(joao, pessoas[1], 4, "Resolveu o problema rápido, bom profissional.");
        createRating(joao, pessoas[3], 5, "Instalação perfeita, muito organizado com a fiação.");
        createRating(joao, pessoas[4], 4, "Trabalho bem feito, preço dentro do esperado.");
        createRating(joao, pessoas[7], 4, "Pontual e eficiente, recomendo.");
        createRating(joao, pessoas[8], 5, "Excelente! Fez a inspeção e encontrou problemas que nem sabia.");
        createRating(joao, pessoas[9], 3, "Bom trabalho, mas demorou mais que o combinado.");

        log.info("=== Seed concluído! ===");
        log.info("Senha padrão: 123456");
        log.info("Usuário de teste: teste@email.com");
    }

    // ---- helpers ----

    private UserEntity createProfessional(String name, String email, String phone, String hash,
                                           ProfessionEntity profession, String bio) {
        var user = userRepository.save(UserEntity.builder()
                .name(name).email(email).phone(phone)
                .role(UserRole.PROFESSIONAL).passwordHash(hash)
                .build());
        professionalProfileRepository.save(ProfessionalProfileEntity.builder()
                .user(user).profession(profession).bio(bio).isAvailable(true)
                .build());
        return user;
    }

    private UserEntity createClient(String name, String email, String phone, String hash,
                                     String taxId, String paymentMethod) {
        var user = userRepository.save(UserEntity.builder()
                .name(name).email(email).phone(phone)
                .role(UserRole.CLIENT).passwordHash(hash)
                .build());
        clientProfileRepository.save(ClientProfileEntity.builder()
                .user(user).taxId(taxId).preferredPaymentMethod(paymentMethod)
                .build());
        return user;
    }

    private ServiceTypeEntity createService(UserEntity owner, String name, String desc, String price) {
        return serviceTypeRepository.save(ServiceTypeEntity.builder()
                .owner(owner).name(name).description(desc).price(new BigDecimal(price))
                .build());
    }

    private void createAppointment(UserEntity professional, UserEntity client,
                                    AppointmentStatus status,
                                    LocalDateTime scheduleDate, LocalDateTime requestDate,
                                    ServiceTypeEntity... services) {
        BigDecimal total = BigDecimal.ZERO;
        for (var svc : services) {
            total = total.add(svc.getPrice());
        }

        var appointment = appointmentRepository.save(AppointmentEntity.builder()
                .professional(professional).client(client)
                .totalAmount(total).status(status)
                .scheduleDate(scheduleDate).requestDate(requestDate)
                .build());

        for (var svc : services) {
            appointmentServiceRepository.save(AppointmentServiceEntity.builder()
                    .appointment(appointment).serviceType(svc)
                    .build());
        }

        createHistory(appointment, professional, client, requestDate);
    }

    private void createHistory(AppointmentEntity appointment, UserEntity professional,
                                UserEntity client, LocalDateTime requestDate) {
        // Criação → PENDING
        saveHistory(appointment, null, AppointmentStatus.PENDING, client, requestDate);

        var status = appointment.getStatus();

        if (status == AppointmentStatus.REJECTED) {
            saveHistory(appointment, AppointmentStatus.PENDING, AppointmentStatus.REJECTED,
                    professional, requestDate.plusDays(1));
            return;
        }

        if (status == AppointmentStatus.APPROVED || status == AppointmentStatus.COMPLETED
                || status == AppointmentStatus.CANCELLED) {
            saveHistory(appointment, AppointmentStatus.PENDING, AppointmentStatus.APPROVED,
                    professional, requestDate.plusDays(1));
        }

        if (status == AppointmentStatus.COMPLETED) {
            saveHistory(appointment, AppointmentStatus.APPROVED, AppointmentStatus.COMPLETED,
                    professional, appointment.getScheduleDate().plusHours(2));
        }

        if (status == AppointmentStatus.CANCELLED) {
            saveHistory(appointment, AppointmentStatus.APPROVED, AppointmentStatus.CANCELLED,
                    client, appointment.getScheduleDate().minusDays(1));
        }
    }

    private void saveHistory(AppointmentEntity appointment, AppointmentStatus previous,
                              AppointmentStatus next, UserEntity changedBy, LocalDateTime changedAt) {
        appointmentHistoryRepository.save(AppointmentHistoryEntity.builder()
                .appointment(appointment).previousStatus(previous).newStatus(next)
                .changedBy(changedBy).changedAt(changedAt)
                .build());
    }

    private void createSchedule(UserEntity professional, DayOfWeek day, String start, String end, int slotMinutes) {
        weeklyScheduleRepository.save(WeeklyScheduleEntity.builder()
                .professional(professional)
                .dayOfWeek(day)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .slotDurationMinutes(slotMinutes)
                .build());
    }

    private void createRating(UserEntity professional, UserEntity client, int score, String comment) {
        ratingRepository.save(RatingEntity.builder()
                .professional(professional).client(client)
                .score(score).comment(comment)
                .build());
    }

    private static LocalDateTime dt(int year, int month, int day, int hour, int min) {
        return LocalDateTime.of(year, month, day, hour, min);
    }
}
