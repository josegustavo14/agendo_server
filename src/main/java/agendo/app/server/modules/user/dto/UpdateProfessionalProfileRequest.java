package agendo.app.server.modules.user.dto;

/**
 * Payload para PATCH /users/me/professional-profile.
 * Todos os campos são opcionais — só os não-nulos são aplicados.
 */
public record UpdateProfessionalProfileRequest(
        Long professionId,
        String bio,
        Boolean isAvailable
) {}
