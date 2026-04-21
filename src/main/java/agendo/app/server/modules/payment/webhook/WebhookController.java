package agendo.app.server.modules.payment.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * recebe eventos POST enviados pela AbacatePay.
 * URL cadastrada no dashboard AbacatePay
 * essa rota deve ficar liberada no SecurityConfig (sem autenticação JWT), pois quem chama é a AbacatePay, não um usuário autenticado.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/abacatepay")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @Value("${abacatepay.webhook-secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader("X-Webhook-Secret") String secret,
            @RequestBody AbacatePayWebhookPayload payload
    ) {

        // valida o secret recebido na query string
        if (!webhookSecret.equals(secret)) {
            log.warn("[Webhook] Secret inválido recebido. Requisição rejeitada.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // processa o evento de forma assíncrona (não bloqueia a resposta)
        try {
            webhookService.handle(payload);
        } catch (Exception ex) {
            // loga mas ainda responde 200 para evitar que a AbacatePa fique re-tentando indefinidamente em caso de bug pontual.
            log.error("[Webhook] Erro ao processar evento: {}", ex.getMessage(), ex);
        }

        // responde 200 OK —> obrigatório para a AbacatePay confirmar entrega
        return ResponseEntity.ok().build();
    }
}

