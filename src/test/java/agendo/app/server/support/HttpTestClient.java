package agendo.app.server.support;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cliente HTTP leve para testes de integração/aceitação, construído sobre o
 * RestClient do Spring (Boot 4). Substitui o RestAssured, que não é
 * compatível com o Groovy 5 trazido pelo Spring Boot 4.
 *
 * Características importantes para testes:
 * - NÃO lança exceção em respostas de erro (4xx/5xx); o status fica
 *   disponível em {@link HttpTestResponse#status()} para asserção, igual ao
 *   comportamento do RestAssured.
 * - O corpo é exposto como texto cru e como JsonNode navegável por "path"
 *   com notação de ponto (ex: "data.id", "clientProfile.taxId").
 *
 * Uso típico:
 *   var resp = http.post("/users", body, token);
 *   assertThat(resp.status()).isEqualTo(201);
 *   assertThat(resp.path("role")).isEqualTo("PROFESSIONAL");
 */
public class HttpTestClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient client;

    public HttpTestClient(int port) {
        this.client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    public HttpTestResponse get(String path, String bearerToken) {
        return exchange(HttpMethod.GET, path, null, bearerHeaders(bearerToken));
    }

    public HttpTestResponse post(String path, String jsonBody, String bearerToken) {
        return exchange(HttpMethod.POST, path, jsonBody, bearerHeaders(bearerToken));
    }

    public HttpTestResponse patch(String path, String jsonBody, String bearerToken) {
        return exchange(HttpMethod.PATCH, path, jsonBody, bearerHeaders(bearerToken));
    }

    /** POST com um header arbitrário (ex: X-Webhook-Secret) em vez de Bearer. */
    public HttpTestResponse postWithHeader(String path, String jsonBody, String headerName, String headerValue) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(headerName, headerValue);
        return exchange(HttpMethod.POST, path, jsonBody, headers);
    }

    private Map<String, String> bearerHeaders(String bearerToken) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (bearerToken != null) {
            headers.put("Authorization", "Bearer " + bearerToken);
        }
        return headers;
    }

    private HttpTestResponse exchange(HttpMethod method, String path, String jsonBody, Map<String, String> headers) {
        RestClient.RequestBodySpec spec = client.method(method).uri(path);

        for (Map.Entry<String, String> h : headers.entrySet()) {
            spec.header(h.getKey(), h.getValue());
        }
        if (jsonBody != null) {
            spec.contentType(MediaType.APPLICATION_JSON).body(jsonBody);
        }

        return spec.exchange((request, response) -> {
            int status = response.getStatusCode().value();
            String bodyText = "";
            try {
                byte[] bytes = response.getBody().readAllBytes();
                if (bytes != null) {
                    bodyText = new String(bytes);
                }
            } catch (Exception ignored) {
                // sem corpo (ex: 204) — bodyText permanece ""
            }
            return new HttpTestResponse(status, bodyText);
        }, false);
    }

    /**
     * Resposta de teste: status + corpo. O corpo pode ser lido como texto ou
     * navegado como JSON.
     */
    public static class HttpTestResponse {

        private final int status;
        private final String body;
        private final JsonNode json;

        HttpTestResponse(int status, String body) {
            this.status = status;
            this.body = body;
            JsonNode parsed = null;
            if (body != null && !body.isBlank()) {
                try {
                    parsed = MAPPER.readTree(body);
                } catch (Exception ignored) {
                    // corpo não-JSON (ex: erro em texto puro) — path() retornará null
                }
            }
            this.json = parsed;
        }

        public int status() {
            return status;
        }

        public String body() {
            return body;
        }

        public JsonNode jsonNode() {
            return json;
        }

        /**
         * Lê um campo do JSON por path com notação de ponto, retornando o
         * valor como String (ou null se ausente). Ex: path("data.id"),
         * path("clientProfile.taxId").
         */
        public String path(String dottedPath) {
            JsonNode node = nodeAt(dottedPath);
            return node == null || node.isNull() ? null : node.asText();
        }

        /** Lê um campo do JSON por path e converte para long. */
        public long pathAsLong(String dottedPath) {
            JsonNode node = nodeAt(dottedPath);
            return node == null ? 0L : node.asLong();
        }

        /** Lê um campo do JSON por path e converte para double. */
        public double pathAsDouble(String dottedPath) {
            JsonNode node = nodeAt(dottedPath);
            return node == null ? 0d : node.asDouble();
        }

        /** Tamanho de um array na raiz do corpo (equivalente ao "size()" do RestAssured). */
        public int rootArraySize() {
            return json != null && json.isArray() ? json.size() : 0;
        }

        private JsonNode nodeAt(String dottedPath) {
            if (json == null) {
                return null;
            }
            JsonNode current = json;
            for (String part : dottedPath.split("\\.")) {
                if (current == null) {
                    return null;
                }
                current = current.get(part);
            }
            return current;
        }
    }
}