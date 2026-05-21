package agendo.app.server.modules.user.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import agendo.app.server.modules.appointment.models.ServiceTypeEntity;
import agendo.app.server.modules.appointment.repository.ServiceTypeRepository;
import agendo.app.server.modules.rating.service.RatingService;
import agendo.app.server.modules.user.dto.ProfessionalSearchResponse;
import agendo.app.server.modules.user.dto.ServiceTypeResponse;
import agendo.app.server.modules.user.models.ProfessionalProfileEntity;
import agendo.app.server.modules.user.repository.ProfessionalProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/professionals")
@RequiredArgsConstructor
@Tag(name = "Professionals", description = "Busca e consulta de profissionais")
public class ProfessionalController {

    private final ProfessionalProfileRepository professionalProfileRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final RatingService ratingService;

    @GetMapping
    @Operation(
        summary = "Buscar profissionais",
        description = "Busca profissionais com filtros opcionais por nome, profissão e tipo de serviço. Retorna o ID do profissional (user_id)."
    )
    @ApiResponse(responseCode = "200", description = "Lista de profissionais retornada com sucesso")
    public ResponseEntity<List<ProfessionalSearchResponse>> search(
            @Parameter(description = "Filtrar por nome do profissional (busca parcial)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filtrar por ID da profissão")
            @RequestParam(required = false) Long professionId,
            @Parameter(description = "Filtrar por nome do tipo de serviço (busca parcial)")
            @RequestParam(required = false) String serviceTypeName) {

        List<ProfessionalProfileEntity> profiles = professionalProfileRepository.searchProfessionals(
                name, professionId, serviceTypeName);

        List<ProfessionalSearchResponse> response = profiles.stream()
                .map(this::toSearchResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar profissional por ID", description = "Retorna os dados do perfil profissional pelo ID do usuário")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profissional encontrado"),
        @ApiResponse(responseCode = "404", description = "Profissional não encontrado")
    })
    public ResponseEntity<ProfessionalSearchResponse> findById(@PathVariable Long id) {
        ProfessionalProfileEntity profile = professionalProfileRepository.findByUserId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional não encontrado: " + id));
        return ResponseEntity.ok(toSearchResponse(profile));
    }

    @GetMapping("/{id}/services")
    @Operation(
        summary = "Listar serviços de um profissional",
        description = "Retorna todos os tipos de serviço oferecidos por um profissional a partir do seu ID (user_id)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de serviços retornada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Profissional não encontrado")
    })
    public ResponseEntity<List<ServiceTypeResponse>> findServicesByProfessionalId(@PathVariable Long id) {
        // Verifica se o profissional existe
        professionalProfileRepository.findByUserId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional não encontrado: " + id));

        List<ServiceTypeEntity> services = serviceTypeRepository.findByOwnerId(id);

        List<ServiceTypeResponse> response = services.stream()
                .map(st -> new ServiceTypeResponse(st.getId(), st.getName(), st.getDescription(), st.getPrice()))
                .toList();

        return ResponseEntity.ok(response);
    }

    private ProfessionalSearchResponse toSearchResponse(ProfessionalProfileEntity profile) {
        Long userId = profile.getUser().getId();
        return new ProfessionalSearchResponse(
            userId,
            profile.getUser().getName(),
            profile.getUser().getPhone(),
            profile.getProfession() != null ? profile.getProfession().getName() : null,
            profile.getBio(),
            profile.getIsAvailable(),
            ratingService.getAverageScore(userId)
        );
    }
}
