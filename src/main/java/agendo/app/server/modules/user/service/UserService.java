package agendo.app.server.modules.user.service;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import agendo.app.server.modules.user.models.ClientProfileEntity;
import agendo.app.server.modules.user.models.ProfessionalProfileEntity;
import agendo.app.server.modules.user.models.ProfessionEntity;
import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.modules.user.models.UserRole;
import agendo.app.server.modules.user.repository.ClientProfileRepository;
import agendo.app.server.modules.user.repository.ProfessionalProfileRepository;
import agendo.app.server.modules.user.repository.ProfessionRepository;
import agendo.app.server.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfessionalProfileRepository professionalProfileRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final ProfessionRepository professionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserEntity createWithProfile(UserEntity user, Long professionId, String bio,
                                        String taxId, String preferredPaymentMethod) {
        UserEntity savedUser;
        try {
            savedUser = userRepository.save(user);
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado: " + user.getEmail());
        }

        if (savedUser.getRole() == UserRole.PROFESSIONAL) {
            ProfessionalProfileEntity.ProfessionalProfileEntityBuilder profileBuilder = ProfessionalProfileEntity.builder()
                    .user(savedUser)
                    .bio(bio);

            if (professionId != null) {
                ProfessionEntity profession = professionRepository.findById(professionId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profissão não encontrada: " + professionId));
                profileBuilder.profession(profession);
            }

            ProfessionalProfileEntity profile = profileBuilder.build();
            professionalProfileRepository.save(profile);
            savedUser.setProfessionalProfile(profile);
        } else if (savedUser.getRole() == UserRole.CLIENT) {
            ClientProfileEntity profile = ClientProfileEntity.builder()
                    .user(savedUser)
                    .taxId(taxId)
                    .preferredPaymentMethod(preferredPaymentMethod)
                    .build();
            clientProfileRepository.save(profile);
            savedUser.setClientProfile(profile);
        }

        return savedUser;
    }

    public UserEntity findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<UserEntity> findAll() {
        return userRepository.findAll();
    }

    public List<UserEntity> findByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    public boolean validatePassword(UserEntity user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public Optional<ProfessionalProfileEntity> findProfessionalProfile(UserEntity user) {
        return professionalProfileRepository.findByUser(user);
    }

    public Optional<ClientProfileEntity> findClientProfile(UserEntity user) {
        return clientProfileRepository.findByUser(user);
    }

    /**
     * Atualiza o perfil profissional do usuário autenticado.
     * Campos nulos no request são ignorados (PATCH-style).
     * Cria o ProfessionalProfileEntity caso ainda não exista (defensivo).
     */
    @Transactional
    public ProfessionalProfileEntity updateProfessionalProfile(
            UserEntity user, Long professionId, String bio, Boolean isAvailable) {

        if (user.getRole() != UserRole.PROFESSIONAL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas profissionais podem atualizar o perfil profissional");
        }

        ProfessionalProfileEntity profile = professionalProfileRepository.findByUser(user)
                .orElseGet(() -> professionalProfileRepository.save(
                        ProfessionalProfileEntity.builder().user(user).build()));

        if (professionId != null) {
            ProfessionEntity profession = professionRepository.findById(professionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Profissão não encontrada: " + professionId));
            profile.setProfession(profession);
        }
        if (bio != null) {
            profile.setBio(bio);
        }
        if (isAvailable != null) {
            profile.setIsAvailable(isAvailable);
        }

        return professionalProfileRepository.save(profile);
    }
}
