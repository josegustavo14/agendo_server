# language: pt
Funcionalidade: Pagamento via PIX
  Como cliente do Agendo
  Quero pagar por um agendamento via PIX
  Para confirmar a contratação do serviço

  Cenário: Cliente gera cobrança PIX para um agendamento
    Dado que existe um profissional "Rita Designer" com email "rita.designer@teste.com"
    E o profissional cadastrou o serviço "Identidade visual" pelo valor de 500.00
    E existe um cliente "Felipe Cliente" com email "felipe.cliente@teste.com"
    E o cliente cria um agendamento com o profissional para o serviço "Identidade visual"
    E o gateway de pagamento está simulado para retornar a cobrança "bill_teste123" com a url "https://abacatepay.com/pay/bill_teste123"
    Quando o cliente solicita o pagamento PIX do agendamento no valor de 50000 centavos
    Então a cobrança deve ser criada com sucesso
    E a url de pagamento retornada deve ser "https://abacatepay.com/pay/bill_teste123"

  Cenário: Webhook de pagamento confirmado atualiza o status da cobrança
    Dado que existe um profissional "Sergio Fotografo" com email "sergio.fotografo@teste.com"
    E o profissional cadastrou o serviço "Sessão de fotos" pelo valor de 400.00
    E existe um cliente "Tatiana Cliente" com email "tatiana.cliente@teste.com"
    E o cliente cria um agendamento com o profissional para o serviço "Sessão de fotos"
    E o gateway de pagamento está simulado para retornar a cobrança "bill_pago456" com a url "https://abacatepay.com/pay/bill_pago456"
    E o cliente solicita o pagamento PIX do agendamento no valor de 40000 centavos
    Quando a AbacatePay notifica que a cobrança "bill_pago456" foi paga
    Então o status do pagamento deve ser atualizado para "PAID"
