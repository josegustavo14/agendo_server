//package agendo.app.server.modules;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.server.LocalServerPort;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//class IntegrationTest {
//
//    @LocalServerPort
//    private int port;
//
//    private HttpClient client;
//    private ObjectMapper mapper;
//    private String baseUrl;
//
//    @BeforeEach
//    void setup() {
//        client = HttpClient.newHttpClient();
//        mapper = new ObjectMapper();
//        baseUrl = "http://localhost:" + port;
//    }
//
//    private String uniqueEmail() {
//        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@email.com";
//    }
//
//    private HttpResponse<String> postJson(String path, String body) throws Exception {
//        return postJson(path, body, null);
//    }
//
//    private HttpResponse<String> postJson(String path, String body, String token) throws Exception {
//        var builder = HttpRequest.newBuilder()
//                .uri(URI.create(baseUrl + path))
//                .header("Content-Type", "application/json");
//        if (token != null) {
//            builder.header("Authorization", "Bearer " + token);
//        }
//        var request = builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
//        return client.send(request, HttpResponse.BodyHandlers.ofString());
//    }
//
//    @Test
//    void testCreateUserAccount() throws Exception {
//        String email = uniqueEmail();
//
//        String body = """
//                {"name":"João Silva","email":"%s","phone":"11999999999","role":"PROFESSIONAL","password":"senha123"}
//                """.formatted(email);
//
//        HttpResponse<String> response = postJson("/users", body);
//
//        assertEquals(201, response.statusCode());
//
//        JsonNode json = mapper.readTree(response.body());
//        assertNotNull(json.get("id"));
//        assertEquals("João Silva", json.get("name").asText());
//        assertEquals(email, json.get("email").asText());
//        assertEquals("11999999999", json.get("phone").asText());
//        assertEquals("PROFESSIONAL", json.get("role").asText());
//        assertNotNull(json.get("token"));
//        assertFalse(json.get("token").asText().isEmpty());
//    }
//
//    @Test
//    void testCreateAppointmentWithToken() throws Exception {
//        // 1. Criar profissional e pegar token
//        String profBody = """
//                {"name":"João Silva","email":"%s","phone":"11999999999","role":"PROFESSIONAL","password":"senha123"}
//                """.formatted(uniqueEmail());
//
//        HttpResponse<String> profResponse = postJson("/users", profBody);
//        assertEquals(201, profResponse.statusCode());
//        JsonNode profJson = mapper.readTree(profResponse.body());
//        String token = profJson.get("token").asText();
//        long professionalId = profJson.get("id").asLong();
//
//        // 2. Criar cliente
//        String clientBody = """
//                {"name":"Maria Santos","email":"%s","phone":"11988888888","role":"CLIENT","password":"senha456"}
//                """.formatted(uniqueEmail());
//
//        HttpResponse<String> clientResponse = postJson("/users", clientBody);
//        assertEquals(201, clientResponse.statusCode());
//        long clientId = mapper.readTree(clientResponse.body()).get("id").asLong();
//
//        // 3. Criar ServiceType com token
//        String stBody = """
//                {"name":"Corte de Cabelo %s","description":"Serviço de corte profissional","price":150.00}
//                """.formatted(UUID.randomUUID().toString().substring(0, 8));
//
//        HttpResponse<String> stResponse = postJson("/service-types", stBody, token);
//        assertEquals(201, stResponse.statusCode());
//        long serviceTypeId = mapper.readTree(stResponse.body()).get("id").asLong();
//
//        // 4. Criar Appointment com token (múltiplos serviços)
//        String appBody = """
//                {"serviceTypeIds":[%d],"professionalId":%d,"clientId":%d,"scheduleDate":"%s"}
//                """.formatted(serviceTypeId, professionalId, clientId, LocalDateTime.now().plusDays(7));
//
//        HttpResponse<String> appResponse = postJson("/appointments", appBody, token);
//        assertEquals(201, appResponse.statusCode());
//
//        JsonNode appJson = mapper.readTree(appResponse.body());
//        assertEquals(150.0, appJson.get("totalAmount").asDouble());
//        assertEquals(clientId, appJson.get("client").get("id").asLong());
//        assertEquals(professionalId, appJson.get("professional").get("id").asLong());
//        assertEquals(1, appJson.get("services").size());
//    }
//}
