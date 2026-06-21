package agendo.app.server.cucumber;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.modules.user.models.UserRole;
import agendo.app.server.modules.user.repository.UserRepository;

/**
 * Persiste usuários diretamente no banco (sem passar pelo endpoint HTTP de
 * cadastro), exatamente como o DataSeeder faz.
 *
 * Usado nas steps que só precisam de um usuário existindo para configurar
 * o cenário (ex: "existe um profissional...") e também nas steps que
 * precisam de um usuário "chamador" autenticado para testar o próprio
 * endpoint POST /users — que agora exige token, já que qualquer usuário
 * logado pode cadastrar outro.
 */
@Component
public class TestUserFactory {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserEntity create(String name, String email, String role) {
        return create(name, email, role, "senha123");
    }

    public UserEntity create(String name, String email, String role, String password) {
        UserEntity user = UserEntity.builder()
                .name(name)
                .email(email)
                .phone("11999990000")
                .role(UserRole.valueOf(role))
                .passwordHash(passwordEncoder.encode(password))
                .token(UUID.randomUUID().toString())
                .build();

        return userRepository.save(user);
    }
}
