package agendo.app.server.modules.user.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import agendo.app.server.modules.user.dto.CreateProfessionRequest;
import agendo.app.server.modules.user.models.ProfessionEntity;
import agendo.app.server.modules.user.repository.ProfessionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Profissões cadastradas no sistema.
 *
 * - GET é público (frontend usa para preencher dropdown e filtros).
 * - POST e DELETE são administrativos: exigem o header X-Admin-Token,
 *   comparado com a propriedade agendo.admin.token (override via env
 *   AGENDO_ADMIN_TOKEN em produção). É um esquema simples de "shared
 *   secret" — adequado para um endpoint de seed via curl, não para
 *   um painel admin com múltiplos usuários.
 */
@RestController
@RequestMapping("/professions")
@RequiredArgsConstructor
@Tag(name = "Professions", description = "Consulta e gerenciamento de profissões")
public class ProfessionController {

    private final ProfessionRepository professionRepository;

    @Value("${agendo.admin.token}")
    private String adminToken;

    @GetMapping
    @Operation(summary = "Listar todas as profissões", description = "Retorna todas as profissões cadastradas. Público.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<ProfessionEntity>> findAll() {
        return ResponseEntity.ok(professionRepository.findAll());
    }

    @PostMapping
    @Operation(
        summary = "Criar profissão (admin)",
        description = "Requer header X-Admin-Token. Idempotente: se já existir profissão com o mesmo nome, retorna 409."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Profissão criada"),
        @ApiResponse(responseCode = "401", description = "Token administrativo inválido"),
        @ApiResponse(responseCode = "409", description = "Profissão já existe"),
        @ApiResponse(responseCode = "400", description = "Nome inválido")
    })
    public ResponseEntity<ProfessionEntity> create(
            @RequestBody CreateProfessionRequest request,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        requireAdmin(token);

        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome da profissão é obrigatório");
        }

        try {
            ProfessionEntity saved = professionRepository.save(
                    ProfessionEntity.builder().name(request.name().trim()).build());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Profissão já cadastrada: " + request.name());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover profissão (admin)", description = "Requer header X-Admin-Token.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Removida"),
        @ApiResponse(responseCode = "401", description = "Token administrativo inválido"),
        @ApiResponse(responseCode = "404", description = "Profissão não encontrada")
    })
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        requireAdmin(token);

        if (!professionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissão não encontrada: " + id);
        }
        professionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(String token) {
        if (token == null || !token.equals(adminToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token administrativo inválido");
        }
    }
}
