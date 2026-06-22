# WhatsUT

Aplicação acadêmica de mensagens distribuídas, construída em Java. O projeto usa **Java RMI** para a comunicação entre um servidor central e clientes com interface gráfica em **JavaFX**.

O WhatsUT permite criar contas, conversar em privado ou em grupo e receber atualizações em tempo real enquanto os usuários estão conectados.

## Funcionalidades

- Cadastro, login e logout de usuários.
- Listagem de usuários cadastrados e usuários online.
- Conversas privadas com histórico de mensagens.
- Criação de grupos com nome, descrição e participantes iniciais.
- Solicitação, aprovação, recusa e cancelamento de entrada em grupos.
- Administração de membros: o administrador pode remover participantes; ao sair, ele encerra o grupo.
- Envio de mensagens de texto e arquivos em conversas privadas e grupos.
- Contadores de mensagens não lidas.
- Callbacks RMI assíncronos para atualizar grupos, solicitações e mensagens sem recarregar a aplicação manualmente.

## Tecnologias

- Java 21
- Maven 3.9+
- Java RMI
- JavaFX 21

## Arquitetura

```text
Cliente JavaFX                         Servidor RMI
┌──────────────────────┐              ┌─────────────────────────┐
│ MainApp e Views      │              │ Server                  │
│ Controllers          │── RMI ──────▶│ ServerService           │
│ RmiClientService     │              │ usuários, grupos e      │
│ ClientService        │◀─ callback ─│ mensagens em memória    │
└──────────────────────┘              └─────────────────────────┘
```

- `ServerRemote` define as operações chamadas pelos clientes.
- `ClientRemote` define os callbacks usados pelo servidor para avisar sobre mudanças e novas mensagens.
- `ServerService` mantém os dados em memória, autenticação e regras de grupos.
- `RmiClientService` é a camada de acesso do cliente JavaFX ao servidor remoto.

## Estrutura do repositório

```text
.
├── README.md
└── whatsut/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/example/
        │   │   ├── Server.java          # Inicializa o registro e o serviço RMI
        │   │   ├── Models/              # Usuários, grupos e mensagens
        │   │   ├── Rmi/                 # Contratos remotos
        │   │   ├── Service/             # Serviços de servidor e callbacks
        │   │   └── Ui/                  # Aplicação JavaFX, views e controllers
        │   └── resources/Styles/        # Estilos da interface
        └── test/java/                   # Cliente de console auxiliar
```

## Pré-requisitos

Instale:

- JDK 21 ou superior;
- Maven 3.9 ou superior.

Verifique a instalação:

```bash
java -version
mvn -version
```

## Como executar

Abra dois terminais na raiz do repositório.

### 1. Inicie o servidor

No primeiro terminal:

```bash
cd whatsut
mvn clean package
java -jar target/whatsut-1.0-SNAPSHOT.jar
```

O servidor cria o registro RMI e publica o serviço em:

```text
rmi://localhost:1099/WhatsUT
```

### 2. Inicie o cliente gráfico

No segundo terminal:

```bash
cd whatsut
mvn javafx:run
```

Cadastre um usuário na tela inicial e faça login. Para testar a troca de mensagens, abra um segundo cliente e entre com outra conta.

## Conexão a um servidor remoto

Por padrão, o cliente procura `localhost:1099/WhatsUT`. É possível alterar o endereço por variáveis de ambiente:

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `WHATSUT_HOST` | `localhost` | Host ou IP do servidor RMI |
| `WHATSUT_RMI_PORT` | `1099` | Porta do registro RMI |
| `WHATSUT_SERVICE_NAME` | `WhatsUT` | Nome do serviço registrado |

O servidor anuncia `localhost` por padrão. Ao executá-lo em outra máquina, defina `RMI_HOSTNAME` com um IP ou hostname acessível pelos clientes.

No PowerShell, por exemplo:

```powershell
$env:RMI_HOSTNAME = "192.168.0.10"
java -jar target/whatsut-1.0-SNAPSHOT.jar
```

No cliente que se conectará a esse servidor:

```powershell
$env:WHATSUT_HOST = "192.168.0.10"
mvn javafx:run
```

## Comandos úteis

```bash
# Compilar e executar as verificações Maven
cd whatsut
mvn test

# Gerar o JAR sem executar testes
mvn clean package -DskipTests

# Executar a interface JavaFX
mvn javafx:run
```

## Limitações conhecidas

- Os dados ficam apenas em memória: ao reiniciar o servidor, contas, grupos e mensagens são perdidos.
- As senhas são armazenadas como hash SHA-256, mas o projeto não é destinado a uso em produção.
- Não há criptografia de ponta a ponta, persistência em banco de dados nem controle avançado de permissões.
- Arquivos são enviados integralmente como arrays de bytes via RMI; arquivos grandes podem afetar a memória e a rede.
