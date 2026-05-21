package agendo.app.server.modules.user.dto;

public record ProfessionalSearchResponse(
    Long id,
    String name,
    String phone,
    String professionName,
    String bio,
    Boolean isAvailable,
    Double rating
) {}
