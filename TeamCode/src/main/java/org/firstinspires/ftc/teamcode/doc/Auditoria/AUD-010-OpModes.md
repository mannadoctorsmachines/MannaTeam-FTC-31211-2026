Claro. A auditoria anterior passa a ser identificada como **AUD-010-OpModes**. O conteúdo e os achados permanecem os mesmos; apenas a identificação sequencial é corrigida.

# AUD-010 — OpModes

## 1. Escopo

Esta auditoria avalia exclusivamente a camada `opmodes` apresentada, considerando o entendimento estabelecido nas auditorias anteriores:

* OpModes atuais são **referência de uso do código existente**;
* não devem ser tratados automaticamente como arquitetura final;
* responsabilidades devem ser analisadas para decidir o que pertence a `Subsystems`, `Commands`, utilitários ou à própria camada de execução;
* a auditoria deve priorizar comportamento, segurança operacional, acoplamento e clareza de responsabilidades.

O material analisado contém:

* `AutoBaseSimples`
* `CalibrarDistanciaRPM`
* `TeleopTest2`
* `NovoTeleop`

Também aparecem no mesmo material `CalcDistAlvo` e `MathU`, já pertencentes a `util`; eles foram considerados apenas quando impactam diretamente os OpModes. 

---

# 2. Resumo executivo

A camada de OpModes apresenta **dois grupos bastante diferentes**.

### Estrutura com direção arquitetural aproveitável

`AutoBaseSimples` já representa uma tentativa clara de transformar o OpMode em uma camada de **orquestração de alto nível**:

```text
OpMode
  └── AutoBaseSimples
       ├── andarCm()
       ├── strafeCm()
       ├── virarGraus()
       ├── virarParaGraus()
       ├── atirar()
       └── pararTudo()
```

Essa direção é compatível com a arquitetura pretendida: o OpMode descreve **o que fazer**, enquanto `Drivetrain`, `Shooter` e `Intake` executam os detalhes.

Por outro lado, `NovoTeleop` ainda contém bastante lógica experimental e apresenta problemas concretos, inclusive um **erro de inicialização potencialmente fatal**.

`CalibrarDistanciaRPM` e `TeleopTest2` são úteis como ferramentas de bancada/referência de uso, mas não deveriam ser confundidos com a implementação definitiva do fluxo competitivo.

---

# 3. Achados

## AUD-010-01 — `NovoTeleop` possui `NullPointerException` na inicialização

### Severidade: **CRÍTICA**

Em `NovoTeleop.init()`:

```java
drive = new Drivetrain();
drive.init(hardwareMap);
odo.init(hardwareMap);
```

Entretanto, não existe inicialização anterior de:

```java
odo = new GobildaOdometry();
```

Portanto:

```java
odo.init(hardwareMap);
```

opera sobre `odo == null`.

O OpMode pode falhar já durante o `init()`.

### Impacto

O `NovoTeleop` não é confiável como OpMode executável no estado apresentado.

### Correção imediata

Se a odometria realmente for necessária:

```java
odo = new GobildaOdometry();
odo.init(hardwareMap);
```

Caso ela não seja usada pelo restante do OpMode, a alternativa arquiteturalmente mais adequada é **remover a dependência da odometria daqui**.

---

# 4. AUD-010-02 — `NovoTeleop` contém lógica de teste residual dentro do controle principal

### Severidade: **ALTA**

O método `updateDrive()` contém:

```java
ligado = !ligado;

if (gamepad1.b && ligado){
    driveForwardCm(10, 10, 0.45, 1000);
    driveForwardCm(-10, -10, 0.45,1000);
} else {
    drive.stop();
}
```

Isso é particularmente problemático porque:

```java
ligado = !ligado;
```

é executado **a cada ciclo do `loop()`**.

Assim, `ligado` não representa um estado controlado por um evento do botão. Ele simplesmente alterna continuamente entre `true` e `false`.

Além disso, o trecho de movimentação é claramente comportamento de teste.

### Impacto

O `NovoTeleop` pode:

* ignorar o controle normal do drive;
* executar movimentos encoderizados inesperados;
* bloquear o ciclo do OpMode durante esses movimentos;
* apresentar comportamento diferente dependendo do instante em que `loop()` é executado.

### Ação

Remover o comportamento experimental de `updateDrive()` e, se necessário, criar um OpMode de teste separado.

---

# 5. AUD-010-03 — `CalibrarDistanciaRPM` contradiz a própria documentação

### Severidade: **ALTA**

A classe declara:

```java
/**
 * OpMode de bancada ...
 * Ele não controla o drive.
 */
```

E no `init()`:

```java
telemetry.addLine("Calibração pronta. O drive não será acionado.");
```

Porém o `loop()` executa:

```java
updateDrive();
```

E `updateDrive()` termina com:

```java
drive.driveMecanum(axial, lateral, yaw, speedMultiplier);
```

Ou seja, esse OpMode **controla efetivamente o drivetrain**.

### Impacto

Existe uma divergência entre:

* intenção documentada;
* telemetria apresentada ao operador;
* comportamento real.

Isso é especialmente perigoso em uma ferramenta de calibração, pois o operador pode acreditar que o robô permanecerá parado.

### Ação

Escolher explicitamente uma das duas intenções.

Se a ferramenta não deve mover o robô:

* remover `updateDrive()`;
* não inicializar `Drivetrain`;
* remover os controles `gamepad1`.

Se o drive deve existir para alinhamento:

* atualizar a documentação;
* deixar explícito que o drive pode girar/mover;
* limitar formalmente quais movimentos são permitidos.

---

# 6. AUD-010-04 — Conflito do botão `X` em `CalibrarDistanciaRPM`

### Severidade: **ALTA**

O mesmo botão:

```java
gamepad2.x
```

é usado para duas responsabilidades diferentes.

Em `updateDrive()`:

```java
alignmentRequested = gamepad2.x;
```

Portanto `X` ativa o alinhamento.

Em `updateSelectedRPM()`:

```java
if (gamepad2.x && !previousX) {
    ...
    shooter.resetEncoders();
}
```

Portanto `X` também zera os encoders do shooter.

### Impacto

Ao pressionar `X`, duas ações conceitualmente independentes podem ocorrer simultaneamente:

1. iniciar alinhamento;
2. zerar encoder.

### Ação

Separar os comandos.

---

# 7. AUD-010-05 — `AutoBaseSimples` é a parte arquiteturalmente mais madura dos OpModes

### Classificação: **POSITIVO / DIRETRIZ**

`AutoBaseSimples` concentra operações de alto nível:

```text
andarCm()
strafeCm()
virarGraus()
virarParaGraus()
atirar()
esperar()
pararTudo()
```

Isso permite que uma rota futura seja descrita sem que cada OpMode precise conhecer os detalhes de:

* encoders;
* timeout de drive;
* controle de giro;
* recuperação de RPM;
* alimentação individual;
* parada dos subsistemas.

Essa direção deve ser preservada.

---

# 8. AUD-010-06 — `AutoBaseSimples.atirar()` está acumulando responsabilidade

### Severidade: **MÉDIA**

`atirar()` atualmente realiza:

```text
validar pré-condições
        ↓
determinar RPM
        ↓
configurar shooter
        ↓
aguardar spin-up
        ↓
alimentar
        ↓
aguardar recuperação
        ↓
próximo disparo
        ↓
parar shooter
```

Isso começa a representar uma **Command/Action de disparo** completa.

### Direção futura

Quando a camada de Commands estiver suficientemente definida:

```text
OpMode
   ↓
ShootCommand
   ↓
Shooter + Intake
```

O OpMode deve expressar a intenção, enquanto a sequência operacional fica encapsulada.

**Não é recomendável fazer essa extração apenas por estética antes de estabilizar o comportamento.**

---

# 9. AUD-010-07 — `AutoBaseSimples` possui bom tratamento de timeout

### Classificação: **POSITIVO**

O movimento utiliza:

```java
RConstants.AUTO_DRIVE_TIMEOUT_MS
```

O giro utiliza:

```java
RConstants.AUTO_TURN_TIMEOUT_MS
```

O disparo possui:

```java
AUTO_SHOOTER_READY_TIMEOUT_MS
AUTO_SHOOTER_RECOVERY_TIMEOUT_MS
AUTO_FEED_TIMEOUT_MS
```

Em caso de falha:

```java
interromperAuto(...)
```

aciona:

```java
pararTudo();
```

Essa relação deve ser preservada.

---

# 10. AUD-010-08 — `AutoBaseSimples` possui boa barreira de execução

### Classificação: **POSITIVO**

A função:

```java
protected boolean podeExecutar()
```

centraliza:

```java
return opModeIsActive() && !autoInterrompido;
```

Ela é utilizada nos loops de:

* movimento;
* giro;
* espera;
* shooter;
* alimentação.

Isso evita que as ações continuem depois que o OpMode deixou de estar ativo ou foi explicitamente interrompido.

---

# 11. AUD-010-09 — `TeleopTest2` apresenta uma arquitetura intermediária útil

### Classificação: **POSITIVO / REFERÊNCIA**

A enumeração:

```java
private enum ShooterMode {
    OFF,
    MANUAL,
    AUTO
}
```

representa uma evolução importante em relação a múltiplos `boolean`.

O método:

```java
updateShooter(...)
```

traduz o modo solicitado em comportamento do `Shooter`.

Essa abordagem deve servir como referência para uma futura consolidação.

---

# 12. AUD-010-10 — `TeleopTest2` separa razoavelmente drive, câmera e mecanismos

A sequência:

```java
updateCameraAndShooterSolution();
updateDrive();
updateMechanisms();
sendTelemetry();
```

é conceitualmente clara.

Ainda há lógica demais dentro do OpMode, mas a organização é significativamente mais compreensível que em `NovoTeleop`.

---

# 13. AUD-010-11 — `CalibrarDistanciaRPM` é ferramenta de calibração

### Classificação: **MÉDIA / ARQUITETURAL**

A classe possui responsabilidades próprias de bancada:

* modificar RPM;
* zerar encoder;
* observar câmera;
* comparar distância;
* testar alimentação;
* observar RPM;
* produzir dados para `ShooterLista`.

Isso é aceitável para uma ferramenta de calibração.

Ela, porém, não deve ser utilizada como modelo da arquitetura competitiva.

---

# 14. AUD-010-12 — Há três estratégias diferentes de controle do shooter

Os OpModes apresentam três implementações:

```text
NovoTeleop
    câmera → distância → tabela → RPM → disparo

TeleopTest2
    OFF / MANUAL / AUTO
          ↓
        shooter

AutoBaseSimples
    distância → tabela → RPM
          ↓
      spin-up
          ↓
      disparos
          ↓
    recuperação
```

Isso demonstra evolução do código, mas também indica duplicação de lógica.

### Diretriz

A arquitetura final deverá possuir uma única fonte para:

* cálculo de RPM;
* estado do shooter;
* critério de pronto;
* sequência de disparo;
* recuperação entre disparos.

Os OpModes devem decidir **quando** executar essas ações.

---

# 15. AUD-010-13 — Ciclo de vida dos OpModes deve ser padronizado

O padrão desejado é:

```text
init()
 ├── criar objetos
 ├── inicializar hardware
 └── estabelecer estado inicial

loop()
 └── usar somente objetos inicializados

stop()
 └── parar/liberar tudo inicializado
```

O `NullPointerException` potencial de `NovoTeleop` demonstra que essa regra ainda não está sendo aplicada de forma consistente.

---

# 16. AUD-010-14 — `NovoTeleop` possui código morto e legado experimental

Exemplos:

```java
private boolean auto = false;
private boolean ultimoB = false;
```

e blocos comentados relacionados a `auto`.

Isso dificulta identificar quais comportamentos ainda fazem parte da especificação real.

### Ação

Realizar uma limpeza controlada:

* remover campos sem uso;
* remover código morto;
* remover blocos comentados de versões anteriores;
* preservar somente comportamentos ainda intencionais.

---

# 17. AUD-010-15 — `NovoTeleop` não deve ser referência arquitetural

Apesar de conter partes úteis, `NovoTeleop` mistura:

* controle competitivo;
* teste;
* código legado;
* lógica experimental.

Além disso, contém o problema de inicialização da odometria e o comportamento experimental associado ao `B`.

Deve ser tratado como **referência histórica de comportamento**, e não como base para a arquitetura final.

---

# 18. Matriz consolidada

| ID         | Achado                                      |  Severidade | Ação                                |
| ---------- | ------------------------------------------- | ----------: | ----------------------------------- |
| AUD-010-01 | `odo.init()` sem inicialização de `odo`     | **Crítica** | Corrigir ou remover dependência     |
| AUD-010-02 | `ligado = !ligado` a cada loop              |    **Alta** | Remover lógica experimental         |
| AUD-010-03 | Calibração afirma não mover drive, mas move |    **Alta** | Corrigir comportamento/documentação |
| AUD-010-04 | `gamepad2.x` alinha e zera encoder          |    **Alta** | Separar comandos                    |
| AUD-010-05 | `AutoBaseSimples` fornece boa abstração     |    Positivo | Preservar direção                   |
| AUD-010-06 | `atirar()` concentra máquina de estados     |       Média | Futuramente extrair para Command    |
| AUD-010-07 | Timeouts do autônomo                        |    Positivo | Preservar                           |
| AUD-010-08 | `podeExecutar()`                            |    Positivo | Preservar                           |
| AUD-010-09 | `ShooterMode`                               |    Positivo | Usar como referência                |
| AUD-010-10 | Separação funcional do `TeleopTest2`        |    Positivo | Evoluir                             |
| AUD-010-11 | Ferramenta de calibração separada           |       Média | Manter separada                     |
| AUD-010-12 | Três implementações de shooter              |       Média | Consolidar                          |
| AUD-010-13 | Ciclo `init/loop/stop` inconsistente        |       Média | Padronizar                          |
| AUD-010-14 | Código morto/experimental                   |       Média | Limpeza controlada                  |
| AUD-010-15 | `NovoTeleop` como legado/referência         |           — | Não usar como base                  |

---

# 19. Arquitetura recomendada

A direção recomendada permanece:

```text
                    OpMode
                      │
             ┌────────┴────────┐
             │                 │
        fluxo da missão     entradas
             │                 │
             ▼                 ▼
        Commands / Actions   controles
             │
      ┌──────┼──────┐
      ▼      ▼      ▼
   Drive  Shooter  Intake
      │      │       │
      └──────┼───────┘
             ▼
         Hardware
```

Para autônomo:

```text
Autonomous OpMode
       │
       ▼
AutoBase / Commands
       │
       ├── andar
       ├── strafe
       ├── girar
       └── atirar
              │
              ▼
         Subsystems
```

Para TeleOp:

```text
TeleOp
  │
  ├── lê gamepad
  ├── determina intenção/modo
  │
  └── solicita ações aos subsystems
```

Para calibração:

```text
Calibration OpMode
       │
       ├── câmera
       ├── shooter
       ├── intake
       └── telemetria
```

---

# 20. Prioridade de correção

### P0

1. Corrigir `odo.init()` ou remover a dependência da odometria.
2. Remover `ligado = !ligado`.
3. Remover a movimentação experimental associada ao `B`.
4. Garantir que o controle normal do drive não seja sobrescrito por teste.

### P1

5. Resolver o conflito do `gamepad2.x`.
6. Corrigir a inconsistência entre documentação e comportamento de `CalibrarDistanciaRPM`.
7. Limpar código morto e blocos comentados de versões anteriores.

### P2

8. Consolidar a lógica do shooter.
9. Evoluir `atirar()` para Command/Action quando a camada estiver definida.
10. Padronizar o ciclo de vida dos OpModes.
11. Reduzir gradualmente lógica de negócio dentro dos OpModes.

---

# 21. Conclusão da AUD-010

**A camada de OpModes está em uma fase de transição arquitetural.**

O principal ponto positivo é `AutoBaseSimples`, que já aponta para o modelo desejado: o OpMode descreve a sequência da missão enquanto os subsistemas executam os detalhes.

`TeleopTest2` apresenta uma evolução relevante através de `ShooterMode`.

`CalibrarDistanciaRPM` possui uma finalidade legítima como ferramenta de bancada, mas deve permanecer separada da arquitetura competitiva.

`NovoTeleop`, por sua vez, deve ser tratado como **código legado/experimental de referência**, pois mistura comportamentos de teste com controle operacional e apresenta problemas concretos de execução.

### Estado recomendado

```text
AutoBaseSimples       → preservar e evoluir
TeleopTest2           → referência para evolução do TeleOp
CalibrarDistanciaRPM  → manter como ferramenta de calibração
NovoTeleop            → corrigir / considerar legado
```

**Próxima etapa:** corrigir primeiro os problemas P0/P1 identificados nesta AUD-010. Depois, a evolução natural é definir quais responsabilidades de `AutoBaseSimples` e `TeleopTest2` devem migrar para `Commands`, sem alterar prematuramente o comportamento de campo.
Claro. A auditoria anterior passa a ser identificada como **AUD-010-OpModes**. O conteúdo e os achados permanecem os mesmos; apenas a identificação sequencial é corrigida.

# AUD-010 — OpModes

## 1. Escopo

Esta auditoria avalia exclusivamente a camada `opmodes` apresentada, considerando o entendimento estabelecido nas auditorias anteriores:

* OpModes atuais são **referência de uso do código existente**;
* não devem ser tratados automaticamente como arquitetura final;
* responsabilidades devem ser analisadas para decidir o que pertence a `Subsystems`, `Commands`, utilitários ou à própria camada de execução;
* a auditoria deve priorizar comportamento, segurança operacional, acoplamento e clareza de responsabilidades.

O material analisado contém:

* `AutoBaseSimples`
* `CalibrarDistanciaRPM`
* `TeleopTest2`
* `NovoTeleop`

Também aparecem no mesmo material `CalcDistAlvo` e `MathU`, já pertencentes a `util`; eles foram considerados apenas quando impactam diretamente os OpModes.

---

# 2. Resumo executivo

A camada de OpModes apresenta **dois grupos bastante diferentes**.

### Estrutura com direção arquitetural aproveitável

`AutoBaseSimples` já representa uma tentativa clara de transformar o OpMode em uma camada de **orquestração de alto nível**:

```text
OpMode
  └── AutoBaseSimples
       ├── andarCm()
       ├── strafeCm()
       ├── virarGraus()
       ├── virarParaGraus()
       ├── atirar()
       └── pararTudo()
```

Essa direção é compatível com a arquitetura pretendida: o OpMode descreve **o que fazer**, enquanto `Drivetrain`, `Shooter` e `Intake` executam os detalhes.

Por outro lado, `NovoTeleop` ainda contém bastante lógica experimental e apresenta problemas concretos, inclusive um **erro de inicialização potencialmente fatal**.

`CalibrarDistanciaRPM` e `TeleopTest2` são úteis como ferramentas de bancada/referência de uso, mas não deveriam ser confundidos com a implementação definitiva do fluxo competitivo.

---

# 3. Achados

## AUD-010-01 — `NovoTeleop` possui `NullPointerException` na inicialização

### Severidade: **CRÍTICA**

Em `NovoTeleop.init()`:

```java
drive = new Drivetrain();
drive.init(hardwareMap);
odo.init(hardwareMap);
```

Entretanto, não existe inicialização anterior de:

```java
odo = new GobildaOdometry();
```

Portanto:

```java
odo.init(hardwareMap);
```

opera sobre `odo == null`.

O OpMode pode falhar já durante o `init()`.

### Impacto

O `NovoTeleop` não é confiável como OpMode executável no estado apresentado.

### Correção imediata

Se a odometria realmente for necessária:

```java
odo = new GobildaOdometry();
odo.init(hardwareMap);
```

Caso ela não seja usada pelo restante do OpMode, a alternativa arquiteturalmente mais adequada é **remover a dependência da odometria daqui**.

---

# 4. AUD-010-02 — `NovoTeleop` contém lógica de teste residual dentro do controle principal

### Severidade: **ALTA**

O método `updateDrive()` contém:

```java
ligado = !ligado;

if (gamepad1.b && ligado){
    driveForwardCm(10, 10, 0.45, 1000);
    driveForwardCm(-10, -10, 0.45,1000);
} else {
    drive.stop();
}
```

Isso é particularmente problemático porque:

```java
ligado = !ligado;
```

é executado **a cada ciclo do `loop()`**.

Assim, `ligado` não representa um estado controlado por um evento do botão. Ele simplesmente alterna continuamente entre `true` e `false`.

Além disso, o trecho de movimentação é claramente comportamento de teste.

### Impacto

O `NovoTeleop` pode:

* ignorar o controle normal do drive;
* executar movimentos encoderizados inesperados;
* bloquear o ciclo do OpMode durante esses movimentos;
* apresentar comportamento diferente dependendo do instante em que `loop()` é executado.

### Ação

Remover o comportamento experimental de `updateDrive()` e, se necessário, criar um OpMode de teste separado.

---

# 5. AUD-010-03 — `CalibrarDistanciaRPM` contradiz a própria documentação

### Severidade: **ALTA**

A classe declara:

```java
/**
 * OpMode de bancada ...
 * Ele não controla o drive.
 */
```

E no `init()`:

```java
telemetry.addLine("Calibração pronta. O drive não será acionado.");
```

Porém o `loop()` executa:

```java
updateDrive();
```

E `updateDrive()` termina com:

```java
drive.driveMecanum(axial, lateral, yaw, speedMultiplier);
```

Ou seja, esse OpMode **controla efetivamente o drivetrain**.

### Impacto

Existe uma divergência entre:

* intenção documentada;
* telemetria apresentada ao operador;
* comportamento real.

Isso é especialmente perigoso em uma ferramenta de calibração, pois o operador pode acreditar que o robô permanecerá parado.

### Ação

Escolher explicitamente uma das duas intenções.

Se a ferramenta não deve mover o robô:

* remover `updateDrive()`;
* não inicializar `Drivetrain`;
* remover os controles `gamepad1`.

Se o drive deve existir para alinhamento:

* atualizar a documentação;
* deixar explícito que o drive pode girar/mover;
* limitar formalmente quais movimentos são permitidos.

---

# 6. AUD-010-04 — Conflito do botão `X` em `CalibrarDistanciaRPM`

### Severidade: **ALTA**

O mesmo botão:

```java
gamepad2.x
```

é usado para duas responsabilidades diferentes.

Em `updateDrive()`:

```java
alignmentRequested = gamepad2.x;
```

Portanto `X` ativa o alinhamento.

Em `updateSelectedRPM()`:

```java
if (gamepad2.x && !previousX) {
    ...
    shooter.resetEncoders();
}
```

Portanto `X` também zera os encoders do shooter.

### Impacto

Ao pressionar `X`, duas ações conceitualmente independentes podem ocorrer simultaneamente:

1. iniciar alinhamento;
2. zerar encoder.

### Ação

Separar os comandos.

---

# 7. AUD-010-05 — `AutoBaseSimples` é a parte arquiteturalmente mais madura dos OpModes

### Classificação: **POSITIVO / DIRETRIZ**

`AutoBaseSimples` concentra operações de alto nível:

```text
andarCm()
strafeCm()
virarGraus()
virarParaGraus()
atirar()
esperar()
pararTudo()
```

Isso permite que uma rota futura seja descrita sem que cada OpMode precise conhecer os detalhes de:

* encoders;
* timeout de drive;
* controle de giro;
* recuperação de RPM;
* alimentação individual;
* parada dos subsistemas.

Essa direção deve ser preservada.

---

# 8. AUD-010-06 — `AutoBaseSimples.atirar()` está acumulando responsabilidade

### Severidade: **MÉDIA**

`atirar()` atualmente realiza:

```text
validar pré-condições
        ↓
determinar RPM
        ↓
configurar shooter
        ↓
aguardar spin-up
        ↓
alimentar
        ↓
aguardar recuperação
        ↓
próximo disparo
        ↓
parar shooter
```

Isso começa a representar uma **Command/Action de disparo** completa.

### Direção futura

Quando a camada de Commands estiver suficientemente definida:

```text
OpMode
   ↓
ShootCommand
   ↓
Shooter + Intake
```

O OpMode deve expressar a intenção, enquanto a sequência operacional fica encapsulada.

**Não é recomendável fazer essa extração apenas por estética antes de estabilizar o comportamento.**

---

# 9. AUD-010-07 — `AutoBaseSimples` possui bom tratamento de timeout

### Classificação: **POSITIVO**

O movimento utiliza:

```java
RConstants.AUTO_DRIVE_TIMEOUT_MS
```

O giro utiliza:

```java
RConstants.AUTO_TURN_TIMEOUT_MS
```

O disparo possui:

```java
AUTO_SHOOTER_READY_TIMEOUT_MS
AUTO_SHOOTER_RECOVERY_TIMEOUT_MS
AUTO_FEED_TIMEOUT_MS
```

Em caso de falha:

```java
interromperAuto(...)
```

aciona:

```java
pararTudo();
```

Essa relação deve ser preservada.

---

# 10. AUD-010-08 — `AutoBaseSimples` possui boa barreira de execução

### Classificação: **POSITIVO**

A função:

```java
protected boolean podeExecutar()
```

centraliza:

```java
return opModeIsActive() && !autoInterrompido;
```

Ela é utilizada nos loops de:

* movimento;
* giro;
* espera;
* shooter;
* alimentação.

Isso evita que as ações continuem depois que o OpMode deixou de estar ativo ou foi explicitamente interrompido.

---

# 11. AUD-010-09 — `TeleopTest2` apresenta uma arquitetura intermediária útil

### Classificação: **POSITIVO / REFERÊNCIA**

A enumeração:

```java
private enum ShooterMode {
    OFF,
    MANUAL,
    AUTO
}
```

representa uma evolução importante em relação a múltiplos `boolean`.

O método:

```java
updateShooter(...)
```

traduz o modo solicitado em comportamento do `Shooter`.

Essa abordagem deve servir como referência para uma futura consolidação.

---

# 12. AUD-010-10 — `TeleopTest2` separa razoavelmente drive, câmera e mecanismos

A sequência:

```java
updateCameraAndShooterSolution();
updateDrive();
updateMechanisms();
sendTelemetry();
```

é conceitualmente clara.

Ainda há lógica demais dentro do OpMode, mas a organização é significativamente mais compreensível que em `NovoTeleop`.

---

# 13. AUD-010-11 — `CalibrarDistanciaRPM` é ferramenta de calibração

### Classificação: **MÉDIA / ARQUITETURAL**

A classe possui responsabilidades próprias de bancada:

* modificar RPM;
* zerar encoder;
* observar câmera;
* comparar distância;
* testar alimentação;
* observar RPM;
* produzir dados para `ShooterLista`.

Isso é aceitável para uma ferramenta de calibração.

Ela, porém, não deve ser utilizada como modelo da arquitetura competitiva.

---

# 14. AUD-010-12 — Há três estratégias diferentes de controle do shooter

Os OpModes apresentam três implementações:

```text
NovoTeleop
    câmera → distância → tabela → RPM → disparo

TeleopTest2
    OFF / MANUAL / AUTO
          ↓
        shooter

AutoBaseSimples
    distância → tabela → RPM
          ↓
      spin-up
          ↓
      disparos
          ↓
    recuperação
```

Isso demonstra evolução do código, mas também indica duplicação de lógica.

### Diretriz

A arquitetura final deverá possuir uma única fonte para:

* cálculo de RPM;
* estado do shooter;
* critério de pronto;
* sequência de disparo;
* recuperação entre disparos.

Os OpModes devem decidir **quando** executar essas ações.

---

# 15. AUD-010-13 — Ciclo de vida dos OpModes deve ser padronizado

O padrão desejado é:

```text
init()
 ├── criar objetos
 ├── inicializar hardware
 └── estabelecer estado inicial

loop()
 └── usar somente objetos inicializados

stop()
 └── parar/liberar tudo inicializado
```

O `NullPointerException` potencial de `NovoTeleop` demonstra que essa regra ainda não está sendo aplicada de forma consistente.

---

# 16. AUD-010-14 — `NovoTeleop` possui código morto e legado experimental

Exemplos:

```java
private boolean auto = false;
private boolean ultimoB = false;
```

e blocos comentados relacionados a `auto`.

Isso dificulta identificar quais comportamentos ainda fazem parte da especificação real.

### Ação

Realizar uma limpeza controlada:

* remover campos sem uso;
* remover código morto;
* remover blocos comentados de versões anteriores;
* preservar somente comportamentos ainda intencionais.

---

# 17. AUD-010-15 — `NovoTeleop` não deve ser referência arquitetural

Apesar de conter partes úteis, `NovoTeleop` mistura:

* controle competitivo;
* teste;
* código legado;
* lógica experimental.

Além disso, contém o problema de inicialização da odometria e o comportamento experimental associado ao `B`.

Deve ser tratado como **referência histórica de comportamento**, e não como base para a arquitetura final.

---

# 18. Matriz consolidada

| ID         | Achado                                      |  Severidade | Ação                                |
| ---------- | ------------------------------------------- | ----------: | ----------------------------------- |
| AUD-010-01 | `odo.init()` sem inicialização de `odo`     | **Crítica** | Corrigir ou remover dependência     |
| AUD-010-02 | `ligado = !ligado` a cada loop              |    **Alta** | Remover lógica experimental         |
| AUD-010-03 | Calibração afirma não mover drive, mas move |    **Alta** | Corrigir comportamento/documentação |
| AUD-010-04 | `gamepad2.x` alinha e zera encoder          |    **Alta** | Separar comandos                    |
| AUD-010-05 | `AutoBaseSimples` fornece boa abstração     |    Positivo | Preservar direção                   |
| AUD-010-06 | `atirar()` concentra máquina de estados     |       Média | Futuramente extrair para Command    |
| AUD-010-07 | Timeouts do autônomo                        |    Positivo | Preservar                           |
| AUD-010-08 | `podeExecutar()`                            |    Positivo | Preservar                           |
| AUD-010-09 | `ShooterMode`                               |    Positivo | Usar como referência                |
| AUD-010-10 | Separação funcional do `TeleopTest2`        |    Positivo | Evoluir                             |
| AUD-010-11 | Ferramenta de calibração separada           |       Média | Manter separada                     |
| AUD-010-12 | Três implementações de shooter              |       Média | Consolidar                          |
| AUD-010-13 | Ciclo `init/loop/stop` inconsistente        |       Média | Padronizar                          |
| AUD-010-14 | Código morto/experimental                   |       Média | Limpeza controlada                  |
| AUD-010-15 | `NovoTeleop` como legado/referência         |           — | Não usar como base                  |

---

# 19. Arquitetura recomendada

A direção recomendada permanece:

```text
                    OpMode
                      │
             ┌────────┴────────┐
             │                 │
        fluxo da missão     entradas
             │                 │
             ▼                 ▼
        Commands / Actions   controles
             │
      ┌──────┼──────┐
      ▼      ▼      ▼
   Drive  Shooter  Intake
      │      │       │
      └──────┼───────┘
             ▼
         Hardware
```

Para autônomo:

```text
Autonomous OpMode
       │
       ▼
AutoBase / Commands
       │
       ├── andar
       ├── strafe
       ├── girar
       └── atirar
              │
              ▼
         Subsystems
```

Para TeleOp:

```text
TeleOp
  │
  ├── lê gamepad
  ├── determina intenção/modo
  │
  └── solicita ações aos subsystems
```

Para calibração:

```text
Calibration OpMode
       │
       ├── câmera
       ├── shooter
       ├── intake
       └── telemetria
```

---

# 20. Prioridade de correção

### P0

1. Corrigir `odo.init()` ou remover a dependência da odometria.
2. Remover `ligado = !ligado`.
3. Remover a movimentação experimental associada ao `B`.
4. Garantir que o controle normal do drive não seja sobrescrito por teste.

### P1

5. Resolver o conflito do `gamepad2.x`.
6. Corrigir a inconsistência entre documentação e comportamento de `CalibrarDistanciaRPM`.
7. Limpar código morto e blocos comentados de versões anteriores.

### P2

8. Consolidar a lógica do shooter.
9. Evoluir `atirar()` para Command/Action quando a camada estiver definida.
10. Padronizar o ciclo de vida dos OpModes.
11. Reduzir gradualmente lógica de negócio dentro dos OpModes.

---

# 21. Conclusão da AUD-010

**A camada de OpModes está em uma fase de transição arquitetural.**

O principal ponto positivo é `AutoBaseSimples`, que já aponta para o modelo desejado: o OpMode descreve a sequência da missão enquanto os subsistemas executam os detalhes.

`TeleopTest2` apresenta uma evolução relevante através de `ShooterMode`.

`CalibrarDistanciaRPM` possui uma finalidade legítima como ferramenta de bancada, mas deve permanecer separada da arquitetura competitiva.

`NovoTeleop`, por sua vez, deve ser tratado como **código legado/experimental de referência**, pois mistura comportamentos de teste com controle operacional e apresenta problemas concretos de execução.

### Estado recomendado

```text
AutoBaseSimples       → preservar e evoluir
TeleopTest2           → referência para evolução do TeleOp
CalibrarDistanciaRPM  → manter como ferramenta de calibração
NovoTeleop            → corrigir / considerar legado
```

**Próxima etapa:** corrigir primeiro os problemas P0/P1 identificados nesta AUD-010. Depois, a evolução natural é definir quais responsabilidades de `AutoBaseSimples` e `TeleopTest2` devem migrar para `Commands`, sem alterar prematuramente o comportamento de campo.
