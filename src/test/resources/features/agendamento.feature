# language: pt
Funcionalidade: Agendamento de serviços
  Como cliente do Agendo
  Quero agendar um serviço com um profissional
  Para ter o trabalho realizado

  Cenário: Cliente cria um agendamento e profissional aprova
    Dado que existe um profissional "João Encanador" com email "joao.encanador@teste.com"
    E o profissional cadastrou o serviço "Conserto de vazamento" pelo valor de 150.00
    E existe um cliente "Pedro Cliente" com email "pedro.cliente@teste.com"
    Quando o cliente cria um agendamento com o profissional para o serviço "Conserto de vazamento"
    Então o agendamento deve ser criado com status "PENDING"
    E o valor total do agendamento deve ser 150.00
    Quando o profissional aprova o agendamento
    Então o status do agendamento deve ser "APPROVED"

  Cenário: Cliente não pode aprovar o próprio agendamento
    Dado que existe um profissional "Lucas Pintor" com email "lucas.pintor@teste.com"
    E o profissional cadastrou o serviço "Pintura de sala" pelo valor de 300.00
    E existe um cliente "Marina Cliente" com email "marina.cliente@teste.com"
    Quando o cliente cria um agendamento com o profissional para o serviço "Pintura de sala"
    E o cliente tenta aprovar o próprio agendamento
    Então a aprovação deve ser rejeitada com status 403
