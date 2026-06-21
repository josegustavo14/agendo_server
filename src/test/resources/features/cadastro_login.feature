# language: pt
Funcionalidade: Cadastro e login de usuários
  Como usuário do Agendo
  Quero me cadastrar e fazer login
  Para acessar as funcionalidades do aplicativo

  Cenário: Cadastro de um novo profissional
    Quando eu cadastro um profissional chamado "Maria Eletricista" com email "maria.eletricista@teste.com"
    Então o cadastro deve ser aceito com sucesso
    E o usuário cadastrado deve ter a role "PROFESSIONAL"
    E um token de autenticação deve ser retornado

  Cenário: Cadastro de um novo cliente
    Quando eu cadastro um cliente chamado "Ana Cliente" com email "ana.cliente@teste.com"
    Então o cadastro deve ser aceito com sucesso
    E o usuário cadastrado deve ter a role "CLIENT"

  Cenário: Login com credenciais válidas
    Dado que um usuário "Carlos Profissional" com email "carlos.login@teste.com" e senha "senha123" está cadastrado
    Quando eu faço login com email "carlos.login@teste.com" e senha "senha123"
    Então o login deve ser aceito com sucesso
    E um token de autenticação deve ser retornado

  Cenário: Login com senha incorreta
    Dado que um usuário "Bia Profissional" com email "bia.login@teste.com" e senha "senha123" está cadastrado
    Quando eu faço login com email "bia.login@teste.com" e senha "senhaErrada"
    Então o login deve ser rejeitado com status 401
