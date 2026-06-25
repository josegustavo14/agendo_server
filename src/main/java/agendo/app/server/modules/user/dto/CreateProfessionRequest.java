package agendo.app.server.modules.user.dto;

/**
 * Payload para POST /professions (somente admin via header X-Admin-Token).
 */
public record CreateProfessionRequest(String name) {}
