# BankTransfer

**Simulador de transferências bancárias com análise de risco**, desenvolvido com Java, Spring Boot, PostgreSQL, React e TypeScript.

O projeto demonstra o processamento de transferências entre contas com **integridade transacional**, **controle de concorrência**, **idempotência** e **regras de prevenção a fraude**. Uma interface web permite cadastrar e consultar contas, realizar transferências, visualizar o histórico e acompanhar operações sinalizadas.

> **Projeto educacional / de portfólio.** Os saldos são fictícios e não representam dinheiro real. A aplicação não dispõe de autenticação, autorização ou integração com instituições financeiras e **não deve ser utilizada em produção**.

## Tecnologias

| Backend | Frontend | Dados e testes |
|---|---|---|
| Java 21, Spring Boot 4.1.2, Spring Web MVC, Spring Data JPA, Bean Validation | React, TypeScript, Vite, CSS | PostgreSQL, Flyway, Maven, JUnit 5, Testcontainers, Docker |

## Funcionalidades

- **Contas:** cadastro com saldo inicial fictício opcional e consulta por ID.
- **Transferências:** débito e crédito entre contas, validação do saldo e de valores monetários (até duas casas decimais).
- **Integridade transacional:** atualização dos saldos na mesma transação do registro da transferência, com bloqueios pessimistas em ordem de ID para reduzir problemas de concorrência.
- **Idempotência:** cabeçalho `Idempotency-Key` para evitar executar novamente uma operação já registrada com a mesma chave. Reutilizar a chave com dados diferentes gera conflito.
- **Análise antifraude:** pontuação calculada antes da movimentação dos saldos; operações de alto risco são recusadas e registradas sem débito/crédito.
- **Histórico:** consulta das transferências enviadas e recebidas por conta.
- **Monitoramento:** listagem de operações de risco moderado/alto, indicadores, filtros e consulta dos alertas associados.
- **Testes:** cenários de regras financeiras, concorrência, idempotência, antifraude e cadastro de contas, utilizando PostgreSQL em contêiner nos testes de integração.

## Como funciona a análise de risco

As regras atuais são demonstrativas e utilizam uma janela móvel de **5 minutos**:

| Regra | Condição | Pontuação |
|---|---|---:|
| Valor elevado | Transferência acima de R$ 5.000,00 | +35 |
| Frequência elevada | Terceira ou posterior tentativa, considerando transferências **concluídas** da conta de origem na janela e a operação atual | +45 |

A classificação utiliza a soma das pontuações:

- **BAIXO:** abaixo de 35 pontos.
- **MODERADO:** de 35 a 69 pontos.
- **ALTO:** 70 pontos ou mais — operação recusada antes de movimentar os saldos.

As transferências recusadas são mantidas no histórico e podem gerar alertas, mas não entram na contagem de transferências concluídas para a regra de frequência.

## Pré-requisitos

- **JDK 21**
- **Node.js e npm** compatíveis com a versão do Vite usada no `frontend/package.json`
- **PostgreSQL** em execução local na porta `5432`
- **Docker Desktop**, ou outro ambiente Docker compatível, para executar os testes com Testcontainers

## Executando localmente

### 1. Preparar o banco de dados

Crie um banco PostgreSQL chamado `banktransfer`. A configuração atual, em `src/main/resources/application.properties`, utiliza:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/banktransfer
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
```

Configure a variável de ambiente `DB_PASSWORD` com a senha do seu PostgreSQL. Por exemplo, no **PowerShell**, para a sessão atual:

```powershell
$env:DB_PASSWORD = "SUA_SENHA_LOCAL"
```

Não coloque senhas reais no código ou no repositório. Se executar pelo IntelliJ, configure `DB_PASSWORD` nas variáveis de ambiente da configuração de execução da aplicação.

O **Flyway** aplica automaticamente as migrações do esquema ao iniciar o backend. Não é necessário criar as tabelas manualmente.

### 2. Iniciar o backend

Na raiz do repositório:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux/macOS, use `./mvnw spring-boot:run`.

Por padrão, a API estará disponível em `http://localhost:8080`.

### 3. Iniciar o frontend

Em outro terminal:

```powershell
cd frontend
npm install
npm run dev
```

Abra o endereço exibido pelo Vite (normalmente `http://localhost:5173`).

Durante o desenvolvimento, o Vite encaminha chamadas iniciadas por `/api` para o backend em `http://localhost:8080`, removendo o prefixo `/api`. **Esse proxy é específico do ambiente de desenvolvimento**; para hospedar o frontend separadamente é necessário configurar o roteamento das chamadas à API.

## Testando a aplicação

1. Abra a aba **Contas** e crie duas contas com saldos iniciais fictícios, por exemplo, R$ 10.000,00 e R$ 1.000,00.
2. Na aba **Transferências**, realize uma transferência entre elas e consulte os novos saldos.
3. Acesse **Histórico** para visualizar operações enviadas e recebidas.
4. Faça uma transferência acima de R$ 5.000,00 para gerar risco **MODERADO** (desde que a regra de frequência não seja acionada).
5. Realize operações adicionais a partir da mesma conta, dentro de cinco minutos, para observar a regra de frequência e, quando ambas forem acionadas, a recusa por risco **ALTO**.
6. Abra o painel **Antifraude**, use o filtro por risco e consulte as regras acionadas em **Ver alertas**.

> O saldo informado no cadastro existe apenas para facilitar a demonstração. Ele não representa depósito real nem gera uma movimentação no histórico. Os resultados de risco dependem das operações já registradas na base e dos horários em que foram realizadas.

## Principais endpoints

As rotas abaixo pertencem ao backend. O prefixo `/api` é usado somente pelo frontend por causa do proxy do Vite.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/contas` | Cadastra uma conta |
| `GET` | `/contas/{id}` | Consulta uma conta |
| `GET` | `/contas/{id}/transferencias` | Histórico da conta |
| `POST` | `/transferencias` | Realiza uma transferência |
| `GET` | `/transferencias/{id}` | Consulta uma transferência |
| `GET` | `/transferencias/sinalizadas` | Lista operações de risco moderado e alto |
| `GET` | `/transferencias/{id}/alertas` | Lista alertas de uma transferência |

### Criar uma conta

`POST /contas`

```json
{
  "titular": "Pedro",
  "saldoInicial": 10000.00
}
```

O `saldoInicial` é opcional: quando omitido, a conta começa com saldo zero.

### Realizar uma transferência

`POST /transferencias`

Cabeçalhos:

```http
Content-Type: application/json
Idempotency-Key: exemplo-transferencia-001
```

Corpo:

```json
{
  "origemId": 1,
  "destinoId": 2,
  "valor": 200.00
}
```

Os IDs devem ser substituídos pelos IDs efetivamente criados no banco.

**Respostas principais:** `201 Created` para uma operação recém-registrada, `200 OK` para repetição idempotente com os mesmos dados e `409 Conflict` quando uma chave já usada é enviada com dados diferentes. Entradas inválidas são rejeitadas; contas e transferências inexistentes retornam `404 Not Found` nas respectivas consultas.

## Testes automatizados

Com o Docker em execução, rode na raiz do projeto:

```powershell
.\mvnw.cmd clean test
```

No Linux/macOS:

```bash
./mvnw clean test
```

O Testcontainers inicia um PostgreSQL isolado para os testes de integração, sem depender dos dados existentes na base local de desenvolvimento.

Para verificar a compilação de produção do frontend:

```powershell
cd frontend
npm run build
```

Esse comando valida os tipos TypeScript e gera os arquivos estáticos na pasta `frontend/dist/`; **não publica** a aplicação na internet.

## Estrutura do projeto

```text
BankTransfer/
├── src/
│   ├── main/
│   │   ├── java/com/pedro/banktransfer/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── enums/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources/
│   │       └── db/migration/
│   └── test/
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── services/
│   │   └── App.tsx
│   └── package.json
├── pom.xml
└── README.md
```

## Limitações e melhorias possíveis

Este projeto é um **simulador**, não um produto bancário. Não inclui autenticação, autorização por usuário, auditoria financeira completa, depósitos/estornos reais ou mecanismos antifraude de produção. As regras de risco são fixas e simplificadas; informações internas também podem ser consultadas pela API, que não possui controle de acesso.

Entre as possíveis evoluções estão documentação OpenAPI/Swagger, Docker Compose para facilitar a instalação, observabilidade e ampliação da cobertura de testes HTTP.
