# AUD-003 — Auditoria do Subsistema Intake

**Projeto:** MannaTeam FTC 31211 — 2026
**Componente:** `subsystems.intake.Intake`
**Configuração:** `subsystems.intake.IntakeConfig`
**Dependências analisadas:** `RConstants`, `commands.Command`, `robot.Robot`
**Status:** APROVADO COM RESSALVAS
**Data da atualização:** 2026-09-28

---

## 1. Escopo

Esta auditoria avalia o subsistema `Intake` quanto a:

* responsabilidade e encapsulamento;
* máquina de estados;
* temporização;
* ciclo de vida;
* API pública;
* integração com configuração global;
* dependência de `update()`;
* integração futura com `Robot` e `Command`;
* segurança e testabilidade.

Arquivos considerados:

```text
subsystems/intake/Intake.java
subsystems/intake/IntakeConfig.java
robot/RConstants.java
commands/Command.java
robot/Robot.java
```

---

## 2. Estrutura encontrada

```text
subsystems/
└── intake/
    ├── Intake.java
    └── IntakeConfig.java
```

O subsistema possui classe própria e encapsula o `DcMotorEx`.

A estrutura física está adequada.

Entretanto, `IntakeConfig` permanece vazio:

```java
public class IntakeConfig {
}
```

Enquanto os parâmetros específicos do mecanismo estão em `RConstants`.

---

## 3. Responsabilidade do subsistema

`Intake` é responsável por:

* obter o motor pelo `HardwareMap`;
* configurar `ZeroPowerBehavior`;
* configurar `RunMode`;
* comandar potência;
* executar alimentação temporizada;
* executar operação contínua;
* executar reversão contínua;
* retornar ao repouso;
* expor o estado operacional.

Essa divisão é adequada.

O `Intake` não tenta decidir quando o shooter está pronto nem implementa lógica de alto nível de disparo. Isso é correto: coordenação entre mecanismos deve pertencer ao nível de comandos/composição.

---

# 4. Configuração confirmada em `RConstants`

Os parâmetros atuais são:

```java
public static final double INTAKE_REST_POWER = 0.0;
public static final double INTAKE_PUSH_POWER = 0.75;
public static final double INTAKE_REVERSE_POWER = -0.75;
public static final long INTAKE_PUSH_TIME_MS = 260;
```

Portanto:

* repouso realmente corresponde a potência `0.0`;
* avanço corresponde a `0.75`;
* reversão corresponde a `-0.75`;
* `pushOne()` possui duração nominal de `260 ms`.

A preocupação anterior sobre `INTAKE_REST_POWER` foi eliminada pela análise de `RConstants`.

O valor de `260 ms`, entretanto, é um parâmetro de calibração mecânica e precisa ser validado fisicamente para garantir que uma operação alimente exatamente uma bola.

---

# 5. Inicialização

A implementação:

```java
feederMotor = hardwareMap.get(
        DcMotorEx.class,
        RConstants.FEEDER
);

feederMotor.setZeroPowerBehavior(
        DcMotor.ZeroPowerBehavior.BRAKE
);

feederMotor.setMode(
        DcMotor.RunMode.RUN_WITHOUT_ENCODER
);

rest();
```

é coerente com o comportamento esperado.

### Avaliação

* `DcMotorEx`: adequado;
* `BRAKE`: adequado para um feeder que deve parar rapidamente;
* `RUN_WITHOUT_ENCODER`: coerente com controle por potência/tempo;
* `rest()` após inicialização: positivo.

Não há necessidade de adicionar encoder apenas para esse comportamento temporizado.

---

# 6. Máquina de estados

O estado atual é representado por:

```java
private boolean pushing = false;
private boolean continuous = false;
private boolean reversing = false;
```

Isso funciona, mas cria uma máquina de estados implícita.

O problema é que os booleanos permitem combinações cujo significado não é diretamente expressado pelo tipo.

Por exemplo:

```text
pushing = true
continuous = true
reversing = true
```

representa, na prática, operação contínua reversa, mas essa semântica precisa ser inferida.

Além disso, `pushing` possui dois significados:

* operação temporizada;
* operação contínua.

### AUD-003-ARCH-01

**Achado:** estado semântico ambíguo devido a múltiplos booleanos.

**Severidade:** Média.

### Recomendação

Em uma refatoração futura, considerar um estado explícito:

```java
enum IntakeState {
    REST,
    PUSHING,
    CONTINUOUS,
    REVERSE
}
```

Não é necessário executar essa refatoração imediatamente. O projeto ainda está construindo a camada de composição (`Robot`) e comandos.

---

# 7. `pushOne()`

Implementação:

```java
public boolean pushOne() {
    if (pushing || continuous) {
        return false;
    }

    reversing = false;
    feederMotor.setPower(RConstants.INTAKE_PUSH_POWER);
    pushStartTime = System.currentTimeMillis();
    pushing = true;
    return true;
}
```

O fluxo é:

```text
pushOne()
   ↓
verifica disponibilidade
   ↓
liga feeder em 0.75
   ↓
registra início
   ↓
pushing = true
   ↓
update()
   ↓
260 ms atingidos
   ↓
rest()
```

### Pontos positivos

* evita iniciar outro `pushOne()` durante uma operação;
* evita iniciar `pushOne()` durante modo contínuo;
* retorna `boolean` indicando aceitação/rejeição;
* centraliza a parada em `rest()`.

### Avaliação

**Funcionalmente adequado.**

O tempo de `260 ms` ainda depende de validação física.

---

# 8. Temporização

O subsistema usa:

```java
System.currentTimeMillis()
```

para medir:

```java
System.currentTimeMillis() - pushStartTime
```

Funciona no runtime normal, mas `currentTimeMillis()` não é a abstração ideal para medição de duração.

### AUD-003-ROB-01

**Achado:** temporização acoplada ao relógio do sistema.

**Severidade:** Baixa/Média.

### Recomendação

Em uma futura melhoria de testabilidade, considerar:

```java
System.nanoTime()
```

ou uma abstração de clock.

Não é uma correção funcional urgente.

---

# 9. Dependência de `update()`

O ciclo temporizado depende de:

```java
public void update()
```

ser chamado regularmente.

O método:

```java
if (continuous || !pushing) {
    return;
}
```

impede que o temporizador seja aplicado ao modo contínuo.

Isso é correto.

Entretanto, se `update()` deixar de ser chamado, um `pushOne()` poderá permanecer ativo.

### AUD-003-CTRL-01

**Achado:** dependência de atualização externa.

**Severidade:** Média.

### Situação arquitetural atual

Foi analisado também:

```java
package org.firstinspires.ftc.teamcode.commands;

public class Command {
}
```

e:

```java
package org.firstinspires.ftc.teamcode.robot;

public class Robot {
}
```

Ambos ainda são esqueletos.

Portanto, atualmente não existe no código analisado um scheduler/compositor que garanta automaticamente:

```text
Robot
  ↓
Intake.update()
```

### Recomendação

Quando `Robot`/Commands forem implementados, o ciclo de atualização deve ser centralizado.

O objetivo deve ser evitar que cada OpMode precise lembrar individualmente de chamar `Intake.update()`.

---

# 10. Operação contínua

`startContinuous()`:

```java
public void startContinuous() {
    continuous = true;
    pushing = true;
    reversing = false;

    feederMotor.setPower(RConstants.INTAKE_PUSH_POWER);
}
```

O comportamento é coerente.

Quando `continuous == true`, `update()` não executa o temporizador.

Isso permite transição segura para operação contínua sem que o timeout de um `pushOne()` desligue o motor.

---

# 11. Reversão contínua

`startReverseContinuous()`:

```java
public void startReverseContinuous() {
    continuous = true;
    pushing = true;
    reversing = true;

    feederMotor.setPower(RConstants.INTAKE_REVERSE_POWER);
}
```

O comportamento é coerente para destravar/liberar uma bola.

A potência é explicitamente configurada como:

```java
INTAKE_REVERSE_POWER = -0.75
```

Não há dependência de temporizador nesse modo.

---

# 12. Parada

Existem:

```java
stopContinuous()
rest()
stop()
```

A implementação converge para o estado de repouso.

`stop()` é particularmente apropriado para integração com ciclo de vida:

```java
public void stop() {
    continuous = false;
    rest();
}
```

Isso garante que o modo contínuo seja encerrado antes da parada do motor.

### Avaliação

**Adequado.**

---

# 13. `isBusy()`

Atualmente:

```java
public boolean isBusy() {
    return pushing;
}
```

Como `pushing` também é `true` durante operação contínua, temos:

```text
startContinuous()
    ↓
isBusy() == true
```

Portanto, `isBusy()` não significa necessariamente:

> existe um `pushOne()` temporizado em execução.

Pode significar:

> o Intake está ativo.

### AUD-003-API-01

**Achado:** semântica de `isBusy()` ambígua.

**Severidade:** Média.

### Recomendação

Antes de alterar, definir semanticamente o contrato.

Se `isBusy()` significar "mecanismo está ativo", a implementação atual é aceitável.

Se significar "push temporizado está em andamento", deve ser alterada.

Uma máquina de estados explícita resolveria essa ambiguidade de maneira mais limpa.

---

# 14. `isContinuous()` e `isReversing()`

Os métodos:

```java
public boolean isContinuous()
public boolean isReversing()
```

possuem significado razoavelmente claro.

As combinações atualmente utilizadas são:

```text
REST
continuous = false
reversing = false

PUSHING
continuous = false
reversing = false

CONTINUOUS
continuous = true
reversing = false

REVERSE
continuous = true
reversing = true
```

O modelo funciona, embora seja implicitamente codificado por flags.

---

# 15. Ciclo de vida

Os métodos de operação pressupõem que:

```java
init(hardwareMap)
```

tenha sido chamado anteriormente.

Caso contrário:

```java
feederMotor.setPower(...)
```

poderá gerar `NullPointerException`.

### AUD-003-ROB-02

**Achado:** contrato de inicialização não protegido.

**Severidade:** Média.

### Recomendação

Não é necessário adicionar verificações defensivas em todos os métodos neste momento.

É suficiente que a futura camada `Robot` estabeleça claramente:

```text
construção
   ↓
init()
   ↓
operação
   ↓
stop()
```

---

# 16. `IntakeConfig`

Atualmente:

```java
public class IntakeConfig {
}
```

Todos os parâmetros continuam em `RConstants`.

### AUD-003-ARCH-02

**Achado:** classe de configuração vazia.

**Severidade:** Baixa.

### Recomendação

Escolher uma das arquiteturas:

### Opção A — manter configuração global

Remover `IntakeConfig` enquanto não houver necessidade real.

### Opção B — configuração por subsistema

Mover:

```text
INTAKE_REST_POWER
INTAKE_PUSH_POWER
INTAKE_REVERSE_POWER
INTAKE_PUSH_TIME_MS
```

para `IntakeConfig`.

A decisão deve seguir o padrão que será estabelecido para os demais subsistemas.

---

# 17. Integração com Shooter

`RConstants` define:

```java
SHOOTER_SPINUP_DELAY_MS = 2000;
```

e documenta que o Intake só deve ser liberado após o tempo mínimo de aceleração do shooter.

O `Intake` não implementa essa regra.

Isso é arquiteturalmente correto.

O Intake deve responder ao comando:

```java
pushOne()
```

e não decidir se o shooter está pronto.

A coordenação esperada é:

```text
Shooter
   ↓
atingiu condição de disparo
   ↓
Command / Robot
   ↓
Intake.pushOne()
```

Portanto, não foi criado um achado contra o `Intake` nesse ponto.

---

# 18. `USE_FEEDER`

`RConstants` possui:

```java
public static final boolean USE_FEEDER = true;
```

O `Intake` não utiliza essa flag diretamente.

Isso é aceitável.

A decisão de quais subsistemas serão instanciados/ativados deve pertencer à composição do robô, e não ser espalhada dentro do subsistema.

Essa responsabilidade deverá ser revisada na auditoria de `Robot`.

---

# 19. Testabilidade

Dependências diretas:

```text
HardwareMap
DcMotorEx
System.currentTimeMillis()
RConstants
```

Isso dificulta testes unitários puros.

O principal ponto de dificuldade é a temporização.

### AUD-003-TEST-01

**Achado:** testabilidade limitada.

**Severidade:** Baixa/Média.

### Recomendação

Somente quando o projeto estabelecer uma estratégia de testes automatizados, considerar abstrair:

* clock;
* hardware;
* configuração.

Não recomenda-se introduzir essas abstrações prematuramente.

---

# 20. Concorrência

Não há sincronização dos estados.

No contexto normal de FTC, isso não é um problema enquanto o subsistema for utilizado pelo thread principal do OpMode.

Não há justificativa, no estado atual, para adicionar `synchronized` ou outras primitivas de concorrência.

---

# 21. Arquivos relacionados ainda incompletos

Foram apresentados:

```java
commands.Command
```

e:

```java
robot.Robot
```

ambos vazios.

Isso é relevante porque o `Intake` já possui uma API operacional razoavelmente completa, mas ainda não há infraestrutura superior para:

* composição;
* atualização centralizada;
* coordenação entre subsistemas;
* ciclo de vida;
* execução de comandos.

Esses pontos devem ser tratados nas auditorias correspondentes, não incorporados artificialmente ao `Intake`.

---

# 22. Matriz de achados

| ID              | Categoria     | Achado                                | Severidade  | Ação                       |
| --------------- | ------------- | ------------------------------------- | ----------- | -------------------------- |
| AUD-003-ARCH-01 | Arquitetura   | Estado baseado em múltiplos booleanos | Média       | Refatoração futura         |
| AUD-003-ARCH-02 | Arquitetura   | `IntakeConfig` vazio                  | Baixa       | Decidir padrão             |
| AUD-003-CTRL-01 | Controle      | `update()` depende de chamada externa | Média       | Resolver em Robot/Commands |
| AUD-003-API-01  | API           | `isBusy()` ambíguo                    | Média       | Definir contrato           |
| AUD-003-ROB-01  | Robustez      | `currentTimeMillis()` para duração    | Baixa/Média | Melhoria futura            |
| AUD-003-ROB-02  | Robustez      | `init()` obrigatório sem proteção     | Média       | Garantir contrato em Robot |
| AUD-003-TEST-01 | Testabilidade | Hardware/clock diretamente acoplados  | Baixa/Média | Melhoria futura            |

---

# 23. Prioridade

## P0 — Bloqueadores

Nenhum.

O `Intake` é funcionalmente utilizável com o ciclo de vida atual do projeto.

## P1 — Importantes

1. Garantir futuramente que `update()` seja chamado pelo ciclo principal.
2. Definir a semântica oficial de `isBusy()`.
3. Estabelecer um modelo de estado mais explícito quando a arquitetura superior estiver consolidada.

## P2 — Melhorias

1. Resolver `IntakeConfig`.
2. Melhorar abstração de tempo.
3. Melhorar testabilidade.
4. Proteger formalmente o ciclo de vida.

---

# 24. Não corrigir prematuramente

Neste estágio, não é recomendada uma grande refatoração do `Intake`.

O projeto ainda apresenta:

```text
Robot   → vazio
Command → vazio
IntakeConfig → vazio
```

Portanto, ainda não existe informação suficiente para definir definitivamente:

* padrão de composição;
* scheduler;
* ciclo de atualização;
* estratégia de configuração;
* padrão de estados dos subsistemas.

A refatoração do `Intake` deve ser revisitada após as auditorias de `Robot` e `Commands`.

---

# 25. Conclusão

## Parecer: APROVADO COM RESSALVAS

O `Intake` apresenta uma implementação funcional, pequena e adequadamente encapsulada.

Os parâmetros atualmente definidos em `RConstants` são coerentes com o código:

```text
REST    = 0.0
PUSH    = 0.75
REVERSE = -0.75
TIME    = 260 ms
```

Não foi identificado defeito funcional crítico no código apresentado.

As principais ressalvas são arquiteturais:

1. estado representado por múltiplos booleanos;
2. dependência de `update()` ainda não garantida por scheduler;
3. semântica de `isBusy()` ambígua;
4. `IntakeConfig` vazio;
5. ciclo de vida dependente de `init()`;
6. testabilidade limitada pela dependência direta de hardware e relógio.

### Decisão

**Manter o `Intake` funcional como está por enquanto.**

Registrar os pontos acima como débitos arquiteturais e revisitá-los após a implementação/auditoria de:

```text
Robot
Commands
Scheduler / ciclo de atualização
```

A próxima refatoração deve ser orientada pela arquitetura global, evitando criar abstrações prematuras dentro do `Intake`.
