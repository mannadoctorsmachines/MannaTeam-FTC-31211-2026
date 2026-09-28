# AUD-002 — Auditoria do Drivetrain

**Projeto:** MannaTeam FTC 31211 — DECODE 2025–2026
**Área:** `subsystems/drivetrain/`
**Status:** Concluída
**Código alterado:** Não

---

## 1. Objetivo

Auditar o subsistema responsável pelo movimento do robô, verificando:

* responsabilidades do `Drivetrain`;
* organização da configuração;
* inicialização dos motores;
* configuração da IMU;
* controle mecanum;
* controle de heading;
* PID;
* movimentação por encoder;
* strafe;
* estados dos motores;
* dependências externas;
* consumidores nos OpModes;
* relação com localização e odometria.

A auditoria busca identificar responsabilidades, inconsistências e pontos de decisão arquitetural antes de qualquer alteração de código.

---

## 2. Escopo analisado

```text
subsystems/
└── drivetrain/
    ├── Drivetrain.java
    └── DrivetrainConfig.java
```

Também foram analisados os principais consumidores do `Drivetrain`, especialmente:

```text
opmodes/
├── AutoBaseSimples.java
├── AutoAzulParede.java
├── AutoCicloSeisBolas.java
├── CalibrarDistanciaRPM.java
├── NovoTeleop.java
└── TeleopTest2.java
```

---

# 3. `Drivetrain.java`

O `Drivetrain` é um subsistema real de controle de movimento, e não apenas uma camada de acesso aos motores.

Suas responsabilidades atuais incluem:

* inicialização dos quatro motores;
* configuração das direções dos motores;
* configuração de `ZeroPowerBehavior`;
* configuração de `RunMode`;
* controle mecanum;
* parada dos motores;
* inicialização da IMU;
* leitura e reset de heading;
* controle de giro por PID;
* movimentação por encoder;
* strafe por encoder;
* controle de `RUN_TO_POSITION`;
* verificação de movimento;
* cálculo de distância percorrida pelos encoders.

A responsabilidade geral do subsistema está, portanto, bem definida.

---

# 4. `DrivetrainConfig.java`

A configuração específica do drivetrain está separada do comportamento.

Contém:

* nomes dos motores;
* presets de potência;
* parâmetros mecânicos;
* cálculo de ticks por centímetro;
* correção de strafe;
* ticks por centímetro de strafe.

A separação é considerada adequada.

Particularmente, a existência de:

```java
STRAFE_CORRECTION
```

como parâmetro independente é positiva, pois representa uma calibração mecânica específica do robô.

---

# 5. Controle mecanum

A implementação possui uma separação adequada entre:

```text
driveMecanum()
        ↓
cálculo das potências
        ↓
setMecanumPowers()
        ↓
motores
```

Os valores são normalizados antes da aplicação aos motores, evitando que uma combinação de axial, lateral e yaw ultrapasse o limite de potência.

O método de aplicação de potência também realiza `clamp`, adicionando uma segunda camada de proteção.

Não foi identificada necessidade de alteração nesta etapa.

---

# 6. IMU e heading

O `Drivetrain` inicializa a IMU e encapsula sua utilização.

Os OpModes não precisam acessar diretamente a IMU. Eles utilizam:

```java
drive.getHeadingDegrees()
```

e métodos relacionados ao controle de heading.

Essa separação é adequada.

Entretanto, a orientação física da IMU está diretamente codificada no `Drivetrain`:

```text
LogoFacingDirection.LEFT
UsbFacingDirection.UP
```

Isso pode ser melhor representado como configuração do drivetrain.

### AUD-002-R05

**Situação:** aberto.

Avaliar posteriormente se a orientação física da IMU deve ser movida para `DrivetrainConfig`.

Nenhuma alteração será feita durante esta auditoria.

---

# 7. Controle PID

O `Drivetrain` instancia:

```java
PIDController
```

utilizando os ganhos definidos em `RConstants`.

O controlador PID, entretanto, permanece desacoplado do hardware e do drivetrain.

Isso é arquiteturalmente adequado:

```text
Drivetrain
    │
    └── utiliza
          │
          ▼
     PIDController
```

e não:

```text
PIDController
    └── conhece Drivetrain
```

O segundo modelo não é desejável.

---

## 7.1 Ganhos do PID

Atualmente os ganhos do giro estão em:

```text
RConstants
```

enquanto outros parâmetros do drivetrain estão em:

```text
DrivetrainConfig
```

Isso cria uma configuração híbrida.

### AUD-002-R04

**Situação:** aberto.

Será necessário decidir posteriormente se os parâmetros específicos do controle de giro pertencem ao `DrivetrainConfig` ou permanecem centralizados.

Nenhuma alteração será feita nesta etapa.

---

## 7.2 Ausência de `dt`

O `PIDController` calcula integral e derivada com base nas diferenças entre ciclos:

```text
integral += error
derivative = error - lastError
```

Não existe cálculo explícito do intervalo de tempo entre as amostras.

Os consumidores analisados utilizam loops com períodos aproximados, incluindo chamadas de `sleep(20)`.

Isso não foi classificado automaticamente como erro.

### AUD-002-R08

**Situação:** aberto.

Avaliar posteriormente a estabilidade e a dependência do PID em relação à frequência do loop.

---

## 7.3 Convenção do cálculo

O drivetrain utiliza:

```java
turnPID.calculate(0.0, -error);
```

Essa chamada é compatível com a convenção atual do código, produzindo o sinal esperado para o controle do giro.

### AUD-002-R09

**Situação:** observação.

Não há evidência suficiente para justificar alteração.

---

# 8. Movimentação por encoder

O drivetrain fornece:

```text
encoderDriveCm()
encoderStrafeCm()
encoderTurnCm()
```

Os consumidores confirmaram que essa API é utilizada pelos autônomos e por código experimental de TeleOp.

Portanto, não se trata de código morto ou apenas planejado.

### AUD-002-R07

**Situação:** confirmado.

A API de movimento por encoder é necessária.

Entretanto, alguns consumidores possuem nomes ou abstrações pouco claros e deverão ser avaliados na auditoria dos OpModes.

---

# 9. Estado dos motores

Os métodos de encoder alteram o modo dos motores para:

```text
STOP_AND_RESET_ENCODER
        ↓
RUN_USING_ENCODER
        ↓
RUN_TO_POSITION
```

Após a execução, não existe restauração automática explícita para:

```text
RUN_WITHOUT_ENCODER
```

Os OpModes acabam fazendo parte desse gerenciamento.

Por exemplo, antes dos giros, `AutoBaseSimples` explicitamente define:

```java
drive.setRunMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
```

Isso demonstra que o estado interno dos motores atualmente atravessa a fronteira do subsistema.

### AUD-002-R02

**Situação:** aberto.

É necessário definir o contrato do `Drivetrain` para os estados de `RunMode` antes de alterar os métodos.

Não será feita uma correção pontual sem essa decisão arquitetural.

---

# 10. `isBusy()`

O método utiliza:

```text
motor 1
   OR
motor 2
   OR
motor 3
   OR
motor 4
```

Ou seja:

```java
isBusy()
```

retorna `true` se qualquer motor ainda estiver ocupado.

A análise de `AutoBaseSimples` mostrou que o consumidor utiliza:

```java
while (... && drive.isBusy() ...)
```

e depois:

```java
boolean terminou = !drive.isBusy();
```

Nesse contexto, o `OR` produz o comportamento desejado: o movimento só é considerado concluído quando nenhum motor estiver ocupado.

### AUD-002-R03

**Situação:** encerrado.

O comportamento atual é coerente com os consumidores analisados.

**Não alterar para `AND`.**

---

# 11. Pinpoint / odometria

O `Drivetrain` inicializa um:

```text
GoBildaPinpointDriver
```

utilizando o hardware:

```text
"odmetry"
```

Entretanto, o objeto não é utilizado pelo restante do `Drivetrain`.

Ao mesmo tempo, o projeto possui:

```text
localization/
├── GobildaOdometry.java
└── Localizer.java
```

Isso indica uma separação de responsabilidades ainda não consolidada.

Existe evidência de que a odometria está sendo tratada como uma responsabilidade de `localization`, enquanto o `Drivetrain` ainda possui uma inicialização residual do Pinpoint.

### AUD-002-R01

**Situação:** aberto.

A questão será resolvida durante a auditoria de:

```text
localization/
```

Não mover ou remover o Pinpoint neste momento.

---

# 12. Strafe

O drivetrain possui uma correção específica:

```java
STRAFE_CORRECTION
```

que é aplicada à conversão de centímetros para ticks.

A própria configuração indica que o valor deve ser calibrado empiricamente devido ao comportamento lateral de rodas mecanum.

Essa responsabilidade está adequadamente localizada em:

```text
DrivetrainConfig
```

### AUD-002-R06

**Situação:** adequado.

Manter a estrutura atual durante a auditoria.

---

# 13. Consumidores

A análise dos consumidores mostrou que o drivetrain é utilizado por diferentes categorias de operação.

### Autônomos

`AutoBaseSimples` utiliza:

```text
encoderDriveCm()
encoderStrafeCm()
getAverageEncoderDistanceCm()
getAverageStrafeDistanceCm()
isBusy()
getHeadingDegrees()
turnToHeading()
setRunMode()
```

Além disso, controla timeouts e fluxo da sequência autônoma.

Isso confirma que o drivetrain fornece primitivas de movimento, enquanto `AutoBaseSimples` organiza a sequência.

Essa divisão é conceitualmente adequada.

---

### TeleOp

`TeleopTest2`, `CalibrarDistanciaRPM` e `NovoTeleop` utilizam:

```text
driveMecanum()
getHeadingDegrees()
getTurnPowerToHeading()
stop()
```

O alinhamento por AprilTag também utiliza o drivetrain para converter o erro angular da câmera em controle de yaw.

---

# 14. Pontos encontrados nos OpModes

Durante a análise dos consumidores apareceram alguns problemas experimentais/legados.

Eles não foram classificados como problemas do `Drivetrain`.

### `NovoTeleop`

Existe lógica experimental envolvendo:

```java
ligado = !ligado;
```

dentro do loop do OpMode.

Também existe um método chamado:

```text
driveForwardCm()
```

que chama:

```text
encoderTurnCm()
```

Isso merece revisão posterior por clareza e comportamento.

---

### `CalibrarDistanciaRPM`

O comentário declara que o drive não será acionado, porém existe uma chamada a:

```text
updateDrive()
```

que efetivamente controla o drivetrain.

Esse ponto pertence à auditoria dos OpModes, não à implementação do drivetrain.

---

# 15. Registro de auditoria

| ID  | Ponto                                            | Situação                             |
| --- | ------------------------------------------------ | ------------------------------------ |
| R01 | Pinpoint dentro do Drivetrain, mas não utilizado | Aberto — AUD-006                     |
| R02 | Estado `RUN_TO_POSITION` após encoder            | Aberto                               |
| R03 | `isBusy()` usa OR                                | Encerrado — comportamento coerente   |
| R04 | PID configurado em `RConstants`                  | Aberto — decisão arquitetural        |
| R05 | Orientação da IMU hardcoded                      | Aberto                               |
| R06 | Correção de strafe isolada                       | Adequado                             |
| R07 | API de encoder                                   | Confirmada                           |
| R08 | PID sem `dt`                                     | Aberto — avaliação técnica           |
| R09 | Convenção `calculate(0, -error)`                 | Manter em observação                 |
| R10 | Código experimental em `NovoTeleop`              | Registrar para auditoria dos OpModes |

---

# 16. Modelo conceitual após AUD-002

A estrutura atual pode ser representada como:

```text
                         ROBOT
                           │
          ┌────────────────┼────────────────┐
          │                │                │
     Configuração     Subsistemas        OpModes
          │                │                │
    RConstants       ┌─────┴─────┐     ┌────┴─────┐
                     │           │     │          │
                 Drivetrain    ...   Auto       TeleOp
                     │
             ┌───────┼────────┐
             │       │        │
           Motor     IMU     PID
                              │
                        PIDController
```

Existe ainda uma fronteira arquitetural pendente:

```text
Drivetrain
    │
    └── Pinpoint
          │
          └── não utilizado

Localization
    └── GobildaOdometry
```

Essa questão será analisada posteriormente no `AUD-006`.

---

# 17. Decisões desta auditoria

### Manter

* `Drivetrain` como subsistema independente;
* `DrivetrainConfig` para configuração específica;
* controle mecanum dentro do drivetrain;
* IMU encapsulada pelo drivetrain;
* controle de heading no drivetrain;
* PID como componente genérico;
* calibração de strafe em `DrivetrainConfig`;
* API de movimento por encoder.

### Não alterar agora

* `RConstants`;
* `PIDController`;
* `RunMode`;
* Pinpoint;
* orientação da IMU;
* APIs de encoder;
* comportamento de `isBusy()`.

As alterações serão decididas somente depois que as dependências relevantes forem auditadas.

---

# 18. Próxima etapa

A próxima auditoria prevista é:

```text
AUD-003 — Intake
```

Escopo:

```text
subsystems/
└── intake/
    ├── Intake.java
    └── IntakeConfig.java
```

Objetivos:

* responsabilidade do Intake;
* motores/servos;
* estados;
* `continuous`;
* `pushOne()`;
* `rest()`;
* `isBusy()`;
* integração com Shooter;
* utilização nos autônomos;
* utilização no TeleOp;
* relação com `RConstants`;
* possíveis responsabilidades indevidas.

---

## Status

```text
AUD-001 — Robot Core       CONCLUÍDO
AUD-002 — Drivetrain       CONCLUÍDO
AUD-003 — Intake           PRÓXIMO
```

**Nenhum arquivo de código foi modificado durante o AUD-002.**

A implementação permanece bloqueada até que as auditorias e decisões arquiteturais necessárias sejam concluídas e validadas.
