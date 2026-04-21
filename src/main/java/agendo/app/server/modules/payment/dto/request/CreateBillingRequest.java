package agendo.app.server.modules.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@JsonInclude(NON_NULL)
public class CreateBillingRequest {

    /**
     * Frequência da cobrança.
     * ONE_TIME → cobrança única.
     * MULTIPLE_PAYMENTS → pode ser paga mais de uma vez.
     */
    private String frequency;

    /**
     * Métodos aceitos: "PIX" e/ou "CARD".
     */
    private List<String> methods;

    /** Produtos incluídos na cobrança. */
    private List<ProductItem> products;

    /** URL de retorno quando o cliente clica em "Voltar". */
    private String returnUrl;

    /** URL de redirecionamento após o pagamento ser concluído. */
    private String completionUrl;

    /** ID de cliente já cadastrado (opcional se `customer` for preenchido). */
    private String customerId;

    /** Dados do cliente; se não existir na AbacatePay ele será criado. */
    private CustomerData customer;

    /** Se cupons são permitidos nesta cobrança. */
    private Boolean allowCoupons;

    /** Lista de códigos de cupom disponíveis (máx 50). */
    private List<String> coupons;

    /** Seu identificador interno para esta cobrança (opcional). */
    private String externalId;

    // ── Subtipos ──────────────────────────────────────────────────────────────

    @Data
    @Builder
    @JsonInclude(NON_NULL)
    public static class ProductItem {
        /** ID do produto no seu sistema. */
        private String externalId;
        private String name;
        private String description;
        /** Quantidade. */
        private Integer quantity;
        /** Preço em centavos (ex: 2000 = R$ 20,00). */
        private Integer price;
    }

    @Data
    @Builder
    @JsonInclude(NON_NULL)
    public static class CustomerData {
        private String name;
        private String cellphone;
        private String email;
        private String taxId;
    }
}

