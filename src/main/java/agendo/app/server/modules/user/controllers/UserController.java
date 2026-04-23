package agendo.app.server.modules.user.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import agendo.app.server.modules.user.dto.CreateUserRequest;
import agendo.app.server.modules.user.dto.LoginRequest;
import agendo.app.server.modules.user.dto.LoginResponse;
import agendo.app.server.modules.user.dto.UserResponse;
import agendo.app.server.modules.user.dto.UserResponse.ClientProfileResponse;
import agendo.app.server.modules.user.dto.UserResponse.ProfessionalProfileResponse;
import agendo.app.server.modules.user.models.ClientProfileEntity;
import agendo.app.server.modules.user.models.ProfessionalProfileEntity;
import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.modules.user.models.UserRole;
import agendo.app.server.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gerenciamento de usuários (profissionais e clientes)")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Criar usuário", description = "Cria um novo usuário com role PROFESSIONAL ou CLIENT, junto com o perfil correspondente")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<UserResponse> create(@RequestBody CreateUserRequest request) {
        UserEntity user = UserEntity.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .role(request.role())
                .passwordHash(userService.encodePassword(request.password()))
                .build();

        UserEntity createdUser = userService.createWithProfile(
                user,
                request.professionId(),
                request.bio(),
                request.taxId(),
                request.preferredPaymentMethod()
        );

        return ResponseEntity.status(201).body(toUserResponse(createdUser));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Faz login com email e senha, retorna o token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Email ou senha inválidos")
    })
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        UserEntity user = userService.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Email ou senha inválidos"));
        if (!userService.validatePassword(user, request.password())) {
            throw new RuntimeException("Email ou senha inválidos");
        }

        LoginResponse response = new LoginResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getToken()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado", description = "Retorna as informações do usuário autenticado pelo token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token inválido ou ausente")
    })
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(toUserResponse(user));
    }

    private UserResponse toUserResponse(UserEntity user) {
        ProfessionalProfileResponse profResponse = null;
        ClientProfileResponse clientResponse = null;

        if (user.getRole() == UserRole.PROFESSIONAL) {
            ProfessionalProfileEntity profile = user.getProfessionalProfile();
            if (profile == null) {
                profile = userService.findProfessionalProfile(user).orElse(null);
            }
            if (profile != null) {
                profResponse = new ProfessionalProfileResponse(
                    profile.getProfession() != null ? profile.getProfession().getId() : null,
                    profile.getProfession() != null ? profile.getProfession().getName() : null,
                    profile.getBio(),
                    profile.getIsAvailable()
                );
            }
        } else if (user.getRole() == UserRole.CLIENT) {
            ClientProfileEntity profile = user.getClientProfile();
            if (profile == null) {
                profile = userService.findClientProfile(user).orElse(null);
            }
            if (profile != null) {
                clientResponse = new ClientProfileResponse(
                    profile.getTaxId(),
                    profile.getPreferredPaymentMethod()
                );
            }
        }

        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPhone(),
            user.getRole().name(),
            user.getToken(),
            profResponse,
            clientResponse
        );
    }

}
