# WhatsUT

WhatsUT é um protótipo de sistema de mensagens em Java que usa **Java RMI** para comunicação entre cliente e servidor. O projeto implementa funcionalidades parecidas com uma aplicação de chat: cadastro e autenticação de usuários, criação de grupos, pedidos de entrada, mensagens em grupo, mensagens privadas, envio de arquivos e notificações assíncronas para clientes conectados.

## Funcionalidades

- Cadastro de usuários com senha armazenada como hash SHA-256.
- Login e logout de usuários autenticados.
- Criação de grupos com administrador.
- Pedido de entrada em grupos e aprovação pelo administrador.
- Remoção de membros de grupos.
- Envio e consulta de mensagens de texto em grupos.
- Envio e consulta de mensagens privadas entre usuários.
- Envio de arquivos para grupos ou conversas privadas.
- Listagem de grupos, membros de um grupo e usuários autenticados.
- Callbacks RMI para notificar clientes sobre:
  - atualização da lista de grupos;
  - novos pedidos de entrada;
  - novas mensagens em grupo;
  - novas mensagens privadas.
- Cliente de console com menu interativo e rotina de teste automático.

## Tecnologias utilizadas

- Java 21
- Java RMI
- Maven
- Docker
- Docker Compose

## Estrutura do projeto

```text
.
├── docker-compose.yml          # Orquestra o serviço do servidor WhatsUT
├── README.md                   # Documentação do projeto
└── whatsut
    ├── Dockerfile              # Build e execução do servidor em container
    ├── pom.xml                 # Configuração Maven
    └── src
        ├── main/java/com/example
        │   ├── Server.java     # Inicialização do servidor RMI
        │   ├── Models          # Entidades do domínio
        │   ├── Rmi             # Interfaces remotas
        │   ├── Service         # Implementações do servidor e callback do cliente
        │   └── Utils           # Utilitários
        └── test/java/Client.java # Cliente de console para testar a aplicação
```

## Requisitos

Para executar localmente:

- JDK 21 ou superior
- Maven 3.9 ou superior

Para executar com Docker:

- Docker
- Docker Compose

## Como executar com Docker Compose

Na raiz do repositório, execute:

```bash
docker compose up --build
```

O servidor RMI será iniciado no endereço padrão:

```text
rmi://localhost:1099/WhatsUT
```

> Observação: se o cliente for executado fora do container e houver problemas de hostname do RMI, configure a variável `RMI_HOSTNAME` para o endereço acessível pelo cliente.

## Como executar localmente

### 1. Compilar o projeto

Entre na pasta do projeto Maven:

```bash
cd whatsut
mvn clean package
```

### 2. Iniciar o servidor

```bash
java -jar target/whatsut-1.0-SNAPSHOT.jar
```

Por padrão, o servidor registra o serviço RMI `WhatsUT` na porta `1099`.

Também é possível configurar o hostname anunciado pelo RMI:

```bash
RMI_HOSTNAME=localhost java -jar target/whatsut-1.0-SNAPSHOT.jar
```

### 3. Executar o cliente de console

Em outro terminal, ainda dentro da pasta `whatsut`, execute:

```bash
mvn test-compile exec:java -Dexec.mainClass=Client
```

Caso prefira executar a classe manualmente, compile os testes e use o classpath gerado pelo Maven.

O cliente aceita, nessa ordem, os argumentos opcionais:

1. host do servidor;
2. porta RMI;
3. nome do serviço RMI.

Exemplo:

```bash
mvn test-compile exec:java -Dexec.mainClass=Client -Dexec.args="localhost 1099 WhatsUT"
```

Também é possível configurar esses valores por variáveis de ambiente:

```bash
WHATSUT_HOST=localhost \
WHATSUT_RMI_PORT=1099 \
WHATSUT_SERVICE_NAME=WhatsUT \
mvn test-compile exec:java -Dexec.mainClass=Client
```

## Menu do cliente

Antes do login, o cliente permite:

- registrar usuário;
- fazer login;
- executar teste automático;
- sair.

Após o login, o menu permite criar grupos, listar grupos, pedir entrada, aprovar membros, enviar e visualizar mensagens, enviar arquivos, listar usuários autenticados, remover membros e fazer logout.

## Teste automático

O cliente possui a opção **Teste automatico**, que cria usuários e grupos temporários, executa operações de mensagens, arquivos, listagens e remoção de membros por 5 ciclos. Essa opção é útil para validar rapidamente o fluxo principal da aplicação.

## API RMI principal

A interface `ServerRemote` expõe operações para:

- `registerUser` — cadastrar usuário;
- `login` e `logout` — controlar sessão;
- `createGroup` — criar grupos;
- `requestJoinGroup` e `approvePendingMember` — gerenciar pedidos de entrada;
- `deleteUserFromGroup` — remover membros;
- `sendPrivateTextMessage` e `sendPrivateFileMessage` — enviar mensagens privadas;
- `sendGroupTextMessage` e `sendGroupFileMessage` — enviar mensagens para grupos;
- `getMessages` e `getPrivateMessages` — consultar histórico;
- `listGroups`, `listGroupUsers` e `listAuthenticatedUsers` — listar dados do sistema.

A interface `ClientRemote` define callbacks usados pelo servidor para atualizar clientes conectados.

## Observações importantes

- Os dados são mantidos em memória; ao reiniciar o servidor, usuários, grupos e mensagens são perdidos.
- O projeto é um protótipo acadêmico/experimental e não implementa persistência, criptografia de ponta a ponta nem controle avançado de permissões.
- Arquivos são transportados como arrays de bytes em objetos RMI.
- O servidor usa mapas concorrentes para armazenar usuários, sessões, grupos e conversas privadas durante a execução.

## Comandos úteis

```bash
# Compilar e executar testes Maven
cd whatsut && mvn test

# Gerar pacote sem executar testes
cd whatsut && mvn clean package -DskipTests

# Subir o servidor em container
docker compose up --build

# Parar e remover containers
docker compose down
```