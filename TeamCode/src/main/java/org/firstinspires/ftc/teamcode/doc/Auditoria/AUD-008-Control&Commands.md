# AUD-008 — Control & Commands

**Projeto:** MannaTeam FTC 31211 — 2026
**Área:** `control` / `commands`
**Status:** Auditoria concluída
**Escopo:** `PIDController` e estrutura de `Command` fornecidas nesta auditoria

---

## 1. Objetivo

Avaliar a implementação atual das camadas:

* `org.firstinspires.ftc.teamcode.control`
* `org.firstinspires.ftc.teamcode.commands`

e determinar:

1. quais responsabilidades já estão implementadas;
2. quais responsabilidades pertencem conceitualmente a essas camadas;
3. problemas técnicos existentes;
4. riscos de evolução da arquitetura;
5. quais decisões devem ser tomadas antes da implementação dos comandos;
6. como essas camadas devem se relacionar com Subsystems e OpModes.

Esta auditoria não assume que a estrutura atual dos OpModes seja a arquitetura final do projeto.

---

# 2. Estado atual

Atualmente existem duas classes relevantes.

### `Command`

```java
package org.firstinspires.ftc.teamcode.commands;

public class Command {
}
```

A classe não possui implementação.

Portanto, atualmente o projeto possui apenas um **placeholder arquitetural** para a camada de Commands.

---

### `PIDController`

A implementação atual possui:

* ganhos `kP`, `kI` e `kD`;
* cálculo de erro;
* acumulador integral;
* limite para o acumulador integral;
* cálculo de derivada;
* tratamento especial da primeira execução;
* `reset()`;
* alteração dos ganhos através de `setPID()`;
* alteração do limite integral;
* acesso ao último erro.

A fórmula implementada é essencialmente:

```text
output = kP × error + kI × integral + kD × derivative
```

com:

```text
error = target - current
```

e o estado integral limitado por `integralLimit`.

---

# 3. Achados

## AUD-008-01 — `Command` ainda não possui responsabilidade definida em código

**Severidade:** Arquitetural / Média

A classe:

```java
public class Command {
}
```

não implementa nenhuma operação.

Isso não é, por si só, um erro de funcionamento, pois não há evidência de que algum código dependa dela atualmente.

Entretanto, existe um risco importante: começar a adicionar métodos diretamente nessa classe sem primeiro definir o modelo de Commands do projeto.

A camada precisa responder claramente perguntas como:

* Um Command possui `initialize()`, `execute()`, `isFinished()` e `end()`?
* Commands possuem acesso direto a Subsystems?
* Quem agenda/executa Commands?
* Um Command pode bloquear a execução?
* Como Commands concorrentes interagem?
* Como uma sequência de Commands é representada?
* Quem controla timeout?
* Como o estado de uma operação é comunicado ao OpMode?

### Conclusão

**Não implementar `Command` genericamente apenas para preencher a classe.**

Primeiro deve ser definido o modelo de execução que será utilizado pelo robô.

---

# 4. AUD-008-02 — `PIDController` possui uma implementação funcional básica

**Severidade:** Informativo

A classe já possui uma implementação coerente de um controlador PID discreto básico.

Pontos positivos:

* separação entre controlador e hardware;
* estado interno encapsulado;
* `reset()` explícito;
* primeiro cálculo não produz derivada baseada em um estado anterior inexistente;
* proteção do acumulador integral;
* possibilidade de alterar os ganhos;
* ausência de dependência direta de `DcMotor`, `HardwareMap` ou qualquer Subsystem.

Essa separação é adequada para a camada `control`.

O `PIDController` não deve conhecer:

* motores;
* servos;
* sensores específicos;
* HardwareMap;
* Subsystems;
* OpModes.

Ele deve receber estado numérico e produzir uma ação numérica.

---

# 5. AUD-008-03 — O PID não utiliza `dt`

**Severidade:** Alta

O principal problema técnico da implementação atual é que o cálculo não considera o intervalo de tempo entre amostras.

Atualmente:

```java
integral += error;
```

e:

```java
derivative = error - lastError;
```

Isso significa que tanto o termo integral quanto o derivativo dependem implicitamente da frequência de execução.

Em um controlador PID físico, normalmente:

```text
integral += error × dt
```

e:

```text
derivative = (error - lastError) / dt
```

Na implementação atual, uma alteração na frequência de execução pode alterar o comportamento efetivo do controlador mesmo mantendo os mesmos ganhos.

Isso é particularmente relevante em FTC porque o tempo entre ciclos não deve ser presumido como perfeitamente constante.

### Consequência

Os ganhos atuais não representam necessariamente parâmetros independentes da frequência de execução.

Um PID ajustado empiricamente para determinado ciclo de execução pode apresentar comportamento diferente caso:

* o loop fique mais lento;
* o loop fique mais rápido;
* haja processamento adicional;
* exista uma operação concorrente;
* o controlador seja reutilizado em outro contexto.

### Recomendação

A próxima versão do controlador deve considerar `dt`, preferencialmente recebendo o tempo de amostragem explicitamente ou utilizando uma fonte de tempo bem definida.

---

# 6. AUD-008-04 — O termo integral é limitado, mas isso não constitui uma proteção completa contra windup

**Severidade:** Média

Existe:

```java
private double integralLimit = 1_000.0;
```

e:

```java
integral = MathU.clamp(
    integral,
    -integralLimit,
    integralLimit
);
```

Isso impede que o acumulador cresça indefinidamente.

É uma proteção válida, mas limitada.

O problema é que o controlador não possui conhecimento sobre eventual saturação da saída.

Por exemplo:

```text
PID calcula 1.8
atuador só aceita 1.0
```

O controlador continua acumulando erro sem saber que o atuador não conseguiu produzir o valor solicitado.

Portanto, o limite atual é apenas um **limite do estado integral**, e não uma estratégia completa de anti-windup.

### Recomendação

Não necessariamente implementar anti-windup sofisticado agora.

Primeiro deve ser definido o contrato do controlador:

* saída normalizada?
* RPM?
* potência?
* posição?
* limite interno ou externo?
* saturação pertence ao PID ou ao Subsystem?

Essa decisão deve ser tomada antes de ampliar a implementação.

---

# 7. AUD-008-05 — Não existe limite de saída no PID

**Severidade:** Média

O `PIDController` retorna diretamente:

```java
(kP * error) + (kI * integral) + (kD * derivative)
```

Não existe limite de saída.

Isso não é necessariamente incorreto.

Na verdade, pode ser arquiteturalmente desejável que o controlador produza um valor livre e que o Subsystem determine os limites físicos do atuador.

Porém, essa responsabilidade precisa ser definida.

Exemplo:

```text
PIDController
      ↓
valor de controle
      ↓
Subsystem
      ↓
limite físico do motor
      ↓
hardware
```

Essa abordagem mantém o PID genérico.

### Recomendação

**Não adicionar `clamp()` automaticamente ao PID.**

Primeiro estabelecer se o contrato de saída do controlador deve ser:

* ilimitado;
* normalizado;
* limitado por configuração;
* limitado pelo consumidor.

Para o projeto atual, manter o controlador independente do hardware é uma direção arquitetural adequada.

---

# 8. AUD-008-06 — Derivada baseada apenas na diferença de erro

**Severidade:** Média

A implementação:

```java
derivative = error - lastError;
```

é uma derivada discreta simplificada.

Sem `dt`, ela representa uma diferença de erro por ciclo, e não uma derivada temporal propriamente dita.

Além disso, como o cálculo utiliza diretamente o erro, alterações abruptas na referência (`target`) também podem produzir um grande termo derivativo.

Isso pode ser aceitável em alguns controladores, mas deve ser considerado durante a definição do comportamento esperado.

### Recomendação

A evolução do PID deve considerar:

* `dt`;
* comportamento na primeira amostra;
* mudanças abruptas de setpoint;
* eventualmente derivada da medição em casos onde isso seja desejável;
* filtragem do termo derivativo caso o ruído dos sensores exija.

Não implementar filtragem apenas por antecipação: ela deve ser introduzida quando houver necessidade observada.

---

# 9. AUD-008-07 — `reset()` é adequado e deve permanecer parte do contrato

**Severidade:** Informativo

A existência de:

```java
public void reset()
```

é importante.

O controlador mantém estado interno:

* integral;
* último erro;
* estado da primeira execução.

Portanto, reutilizar uma instância sem reset pode carregar estado de uma operação anterior.

O comportamento atual de:

```java
setPID(...)
```

também chama:

```java
reset();
```

Isso é coerente.

### Atenção futura

Caso o projeto passe a alterar ganhos durante uma operação, deve ser decidido explicitamente se alterar os ganhos deve ou não zerar o estado.

Não é necessariamente correto assumir que todo `setPID()` deve sempre resetar o controlador.

Por enquanto, o comportamento atual é aceitável.

---

# 10. AUD-008-08 — `control` e `commands` devem ter responsabilidades diferentes

**Severidade:** Arquitetural / Alta

A auditoria indica uma separação conceitual importante.

## `control`

Deve conter mecanismos matemáticos e de controle.

Exemplos:

* PID;
* controladores de velocidade;
* controladores de posição;
* filtros;
* rampas;
* funções de controle;
* utilitários relacionados ao controle de sistemas.

Essas classes devem ser, na medida do possível, independentes de hardware.

---

## `commands`

Deve representar **ações ou operações do robô**.

Exemplos futuros:

```text
IntakeCommand
ShootCommand
MoveToPositionCommand
AlignToAprilTagCommand
SetShooterVelocityCommand
```

Um Command deve coordenar ações de um ou mais Subsystems.

Ele não deve substituir o Subsystem.

---

# 11. AUD-008-09 — Relação esperada entre as camadas

A arquitetura desejada deve caminhar para algo próximo de:

```text
OpMode
   │
   ▼
Commands / sequência de ações
   │
   ▼
Subsystems
   │
   ├── Intake
   ├── Shooter
   ├── Drivetrain
   ├── Localization
   └── Vision
        │
        ▼
     Hardware

Control
   │
   ├── PIDController
   ├── outros controladores
   └── utilidades de controle
```

`Control` não precisa necessariamente ficar acima ou abaixo dos Subsystems como uma camada rígida.

Um Subsystem pode utilizar um controlador:

```text
Shooter
   │
   └── PIDController
```

enquanto um Command pode solicitar:

```text
ShootCommand
       │
       ▼
    Shooter
       │
       ▼
 PIDController
       │
       ▼
    Motor
```

Essa divisão mantém cada responsabilidade relativamente localizada.

---

# 12. AUD-008-10 — Commands não devem incorporar lógica de hardware

**Severidade:** Alta

Um Command futuro não deve fazer diretamente:

```java
hardwareMap.get(...)
```

nem manipular motores diretamente como regra geral.

Exemplo indesejado:

```java
motor.setPower(...);
servo.setPosition(...);
```

dentro de um Command.

A responsabilidade do Command deve ser expressar a operação:

```text
"acionar Intake"
"disparar"
"mover para posição"
"alinhar"
```

e delegar a implementação ao Subsystem.

Isso evita que a lógica operacional se espalhe pelos OpModes e Commands.

---

# 13. AUD-008-11 — Não há Scheduler/Executor de Commands

**Severidade:** Arquitetural / Alta

Com o código fornecido, não existe infraestrutura para executar Commands.

Ainda não está definido:

* como um Command começa;
* como é executado a cada ciclo;
* quando termina;
* como é cancelado;
* como ocorre concorrência;
* como conflitos entre Commands são tratados;
* como sequências são executadas;
* como timeouts funcionam.

Consequentemente, criar vários Commands agora sem definir o mecanismo de execução provavelmente produziria uma arquitetura inconsistente.

### Recomendação

Antes de criar uma quantidade significativa de Commands, definir um modelo mínimo de execução.

Não é necessário construir um framework complexo.

Um scheduler simples e adequado ao robô pode ser suficiente.

---

# 14. AUD-008-12 — Não adotar framework de Commands automaticamente

**Severidade:** Decisão arquitetural

A existência da pasta:

```text
commands/
```

não implica que o projeto precise reproduzir uma arquitetura completa de frameworks externos.

A decisão deve considerar:

* tamanho do projeto;
* quantidade de OpModes;
* quantidade de sequências;
* necessidade de concorrência;
* complexidade dos Subsystems;
* facilidade de depuração em competição;
* overhead introduzido pela abstração.

O objetivo deve ser obter uma separação clara de responsabilidades, e não maximizar a quantidade de abstrações.

---

# 15. AUD-008-13 — `PIDController` não deve conhecer o contexto do robô

**Severidade:** Informativo / Arquitetural

A implementação atual tem uma característica positiva:

```text
PIDController
```

não depende de:

```text
HardwareMap
DcMotor
DcMotorEx
Servo
IMU
Vision
Subsystem
OpMode
```

Isso deve ser preservado.

O controlador pode ser reutilizado para:

* velocidade do Shooter;
* posição de mecanismo;
* controle de trajetória;
* outros mecanismos que necessitem de realimentação.

A especialização deve ocorrer no consumidor, não no controlador.

---

# 16. Limitações da auditoria

Esta auditoria foi feita sobre o código fornecido para:

```text
commands/Command.java
control/PIDController.java
```

Portanto, não é possível concluir a partir desses arquivos isoladamente:

* quais Commands já são necessários;
* quais controladores adicionais são necessários;
* quais OpModes utilizam PID;
* quais ganhos estão sendo utilizados;
* quais limites físicos dos atuadores devem ser aplicados;
* se existe alguma infraestrutura adicional de execução fora dos arquivos apresentados.

Essas questões devem ser verificadas durante a integração das próximas auditorias.

---

# 17. Decisões arquiteturais recomendadas

## Decisão 1 — Manter `control` independente de hardware

**Recomendado.**

`PIDController` deve permanecer genérico.

---

## Decisão 2 — Commands coordenam Subsystems

**Recomendado.**

Commands não devem se tornar uma segunda implementação dos Subsystems.

---

## Decisão 3 — Não preencher `Command` antes de definir o ciclo de vida

**Recomendado.**

Definir primeiro:

```text
initialize()
execute()
isFinished()
end()
```

ou outro contrato mínimo equivalente.

---

## Decisão 4 — Introduzir `dt` no PID

**Recomendado.**

A implementação atual deve ser considerada um PID discreto simplificado.

Antes de utilizar o controlador como base para controle preciso, deve ser definida uma estratégia de tempo.

---

## Decisão 5 — Não implementar recursos avançados prematuramente

Não há necessidade imediata de adicionar:

* filtros complexos;
* feedforward;
* múltiplos modos de PID;
* cascatas;
* scheduler sofisticado;
* sistema completo de requirements;
* abstrações genéricas de sequência.

Esses recursos devem surgir de necessidades reais identificadas nos Subsystems e nos OpModes.

---

# 18. Plano de evolução

### Etapa 1 — Definir contrato de Command

Definir o ciclo de vida mínimo.

Exemplo conceitual:

```text
initialize()
execute()
isFinished()
end()
```

---

### Etapa 2 — Definir executor/scheduler mínimo

Responsável por:

```text
Command
    ↓
initialize()
    ↓
execute() repetidamente
    ↓
isFinished()
    ↓
end()
```

---

### Etapa 3 — Integrar Commands aos Subsystems

Exemplos:

```text
ShootCommand
    ↓
Shooter

IntakeCommand
    ↓
Intake

Drive/Move Command
    ↓
Drivetrain
```

---

### Etapa 4 — Revisar `PIDController`

Introduzir `dt` e testar o comportamento.

Depois avaliar:

* anti-windup;
* limites;
* derivada;
* tolerância;
* estabilidade;
* necessidade de feedforward.

---

### Etapa 5 — Migrar responsabilidades dos OpModes

Os OpModes devem progressivamente assumir o papel de composição/entrada:

```text
OpMode
    ↓
Command
    ↓
Subsystem
    ↓
Hardware
```

em vez de concentrar lógica operacional diretamente no OpMode.

---

# 19. Resultado da auditoria

| Componente                     | Estado                   | Avaliação                                 |
| ------------------------------ | ------------------------ | ----------------------------------------- |
| `PIDController`                | Implementado             | Base funcional, mas simplificada          |
| `Command`                      | Placeholder              | Arquitetura ainda não definida            |
| Scheduler                      | Não identificado         | Necessário antes de escalar Commands      |
| Lifecycle de Command           | Não definido             | Deve ser especificado                     |
| Integração Command → Subsystem | Não implementada         | Próxima etapa                             |
| PID com `dt`                   | Não implementado         | Necessário para controle temporal robusto |
| Anti-windup completo           | Não implementado         | Avaliar posteriormente                    |
| Limite de saída do PID         | Não implementado         | Decisão arquitetural pendente             |
| Dependência de hardware no PID | Não existe               | Ponto positivo                            |
| Separação Control/Subsystem    | Conceitualmente possível | Deve ser preservada                       |

---

# 20. Conclusão

A camada `control` já possui uma primeira implementação útil através do `PIDController`, mas o controlador deve ser tratado como uma **implementação PID discreta simplificada**, e não ainda como uma solução completa de controle temporal.

O principal trabalho desta auditoria não é adicionar funcionalidades ao PID imediatamente, mas estabelecer a separação de responsabilidades:

```text
Control
    → matemática e algoritmos de controle

Subsystem
    → estado e operação dos mecanismos

Command
    → coordenação de operações dos Subsystems

OpMode
    → composição/execução de alto nível
```

A classe `Command` vazia não deve ser preenchida arbitrariamente. Antes disso, o projeto deve definir um contrato mínimo de ciclo de vida e um mecanismo simples de execução.

O próximo passo natural é, portanto, **definir a arquitetura mínima de Commands/Scheduler e então conectar essa arquitetura aos Subsystems já auditados**, sem antecipar um framework excessivamente complexo.
# AUD-008 — Control & Commands

**Projeto:** MannaTeam FTC 31211 — 2026
**Área:** `control` / `commands`
**Status:** Auditoria concluída
**Escopo:** `PIDController` e estrutura de `Command` fornecidas nesta auditoria

---

## 1. Objetivo

Avaliar a implementação atual das camadas:

* `org.firstinspires.ftc.teamcode.control`
* `org.firstinspires.ftc.teamcode.commands`

e determinar:

1. quais responsabilidades já estão implementadas;
2. quais responsabilidades pertencem conceitualmente a essas camadas;
3. problemas técnicos existentes;
4. riscos de evolução da arquitetura;
5. quais decisões devem ser tomadas antes da implementação dos comandos;
6. como essas camadas devem se relacionar com Subsystems e OpModes.

Esta auditoria não assume que a estrutura atual dos OpModes seja a arquitetura final do projeto.

---

# 2. Estado atual

Atualmente existem duas classes relevantes.

### `Command`

```java
package org.firstinspires.ftc.teamcode.commands;

public class Command {
}
```

A classe não possui implementação.

Portanto, atualmente o projeto possui apenas um **placeholder arquitetural** para a camada de Commands.

---

### `PIDController`

A implementação atual possui:

* ganhos `kP`, `kI` e `kD`;
* cálculo de erro;
* acumulador integral;
* limite para o acumulador integral;
* cálculo de derivada;
* tratamento especial da primeira execução;
* `reset()`;
* alteração dos ganhos através de `setPID()`;
* alteração do limite integral;
* acesso ao último erro.

A fórmula implementada é essencialmente:

```text
output = kP × error + kI × integral + kD × derivative
```

com:

```text
error = target - current
```

e o estado integral limitado por `integralLimit`.

---

# 3. Achados

## AUD-008-01 — `Command` ainda não possui responsabilidade definida em código

**Severidade:** Arquitetural / Média

A classe:

```java
public class Command {
}
```

não implementa nenhuma operação.

Isso não é, por si só, um erro de funcionamento, pois não há evidência de que algum código dependa dela atualmente.

Entretanto, existe um risco importante: começar a adicionar métodos diretamente nessa classe sem primeiro definir o modelo de Commands do projeto.

A camada precisa responder claramente perguntas como:

* Um Command possui `initialize()`, `execute()`, `isFinished()` e `end()`?
* Commands possuem acesso direto a Subsystems?
* Quem agenda/executa Commands?
* Um Command pode bloquear a execução?
* Como Commands concorrentes interagem?
* Como uma sequência de Commands é representada?
* Quem controla timeout?
* Como o estado de uma operação é comunicado ao OpMode?

### Conclusão

**Não implementar `Command` genericamente apenas para preencher a classe.**

Primeiro deve ser definido o modelo de execução que será utilizado pelo robô.

---

# 4. AUD-008-02 — `PIDController` possui uma implementação funcional básica

**Severidade:** Informativo

A classe já possui uma implementação coerente de um controlador PID discreto básico.

Pontos positivos:

* separação entre controlador e hardware;
* estado interno encapsulado;
* `reset()` explícito;
* primeiro cálculo não produz derivada baseada em um estado anterior inexistente;
* proteção do acumulador integral;
* possibilidade de alterar os ganhos;
* ausência de dependência direta de `DcMotor`, `HardwareMap` ou qualquer Subsystem.

Essa separação é adequada para a camada `control`.

O `PIDController` não deve conhecer:

* motores;
* servos;
* sensores específicos;
* HardwareMap;
* Subsystems;
* OpModes.

Ele deve receber estado numérico e produzir uma ação numérica.

---

# 5. AUD-008-03 — O PID não utiliza `dt`

**Severidade:** Alta

O principal problema técnico da implementação atual é que o cálculo não considera o intervalo de tempo entre amostras.

Atualmente:

```java
integral += error;
```

e:

```java
derivative = error - lastError;
```

Isso significa que tanto o termo integral quanto o derivativo dependem implicitamente da frequência de execução.

Em um controlador PID físico, normalmente:

```text
integral += error × dt
```

e:

```text
derivative = (error - lastError) / dt
```

Na implementação atual, uma alteração na frequência de execução pode alterar o comportamento efetivo do controlador mesmo mantendo os mesmos ganhos.

Isso é particularmente relevante em FTC porque o tempo entre ciclos não deve ser presumido como perfeitamente constante.

### Consequência

Os ganhos atuais não representam necessariamente parâmetros independentes da frequência de execução.

Um PID ajustado empiricamente para determinado ciclo de execução pode apresentar comportamento diferente caso:

* o loop fique mais lento;
* o loop fique mais rápido;
* haja processamento adicional;
* exista uma operação concorrente;
* o controlador seja reutilizado em outro contexto.

### Recomendação

A próxima versão do controlador deve considerar `dt`, preferencialmente recebendo o tempo de amostragem explicitamente ou utilizando uma fonte de tempo bem definida.

---

# 6. AUD-008-04 — O termo integral é limitado, mas isso não constitui uma proteção completa contra windup

**Severidade:** Média

Existe:

```java
private double integralLimit = 1_000.0;
```

e:

```java
integral = MathU.clamp(
    integral,
    -integralLimit,
    integralLimit
);
```

Isso impede que o acumulador cresça indefinidamente.

É uma proteção válida, mas limitada.

O problema é que o controlador não possui conhecimento sobre eventual saturação da saída.

Por exemplo:

```text
PID calcula 1.8
atuador só aceita 1.0
```

O controlador continua acumulando erro sem saber que o atuador não conseguiu produzir o valor solicitado.

Portanto, o limite atual é apenas um **limite do estado integral**, e não uma estratégia completa de anti-windup.

### Recomendação

Não necessariamente implementar anti-windup sofisticado agora.

Primeiro deve ser definido o contrato do controlador:

* saída normalizada?
* RPM?
* potência?
* posição?
* limite interno ou externo?
* saturação pertence ao PID ou ao Subsystem?

Essa decisão deve ser tomada antes de ampliar a implementação.

---

# 7. AUD-008-05 — Não existe limite de saída no PID

**Severidade:** Média

O `PIDController` retorna diretamente:

```java
(kP * error) + (kI * integral) + (kD * derivative)
```

Não existe limite de saída.

Isso não é necessariamente incorreto.

Na verdade, pode ser arquiteturalmente desejável que o controlador produza um valor livre e que o Subsystem determine os limites físicos do atuador.

Porém, essa responsabilidade precisa ser definida.

Exemplo:

```text
PIDController
      ↓
valor de controle
      ↓
Subsystem
      ↓
limite físico do motor
      ↓
hardware
```

Essa abordagem mantém o PID genérico.

### Recomendação

**Não adicionar `clamp()` automaticamente ao PID.**

Primeiro estabelecer se o contrato de saída do controlador deve ser:

* ilimitado;
* normalizado;
* limitado por configuração;
* limitado pelo consumidor.

Para o projeto atual, manter o controlador independente do hardware é uma direção arquitetural adequada.

---

# 8. AUD-008-06 — Derivada baseada apenas na diferença de erro

**Severidade:** Média

A implementação:

```java
derivative = error - lastError;
```

é uma derivada discreta simplificada.

Sem `dt`, ela representa uma diferença de erro por ciclo, e não uma derivada temporal propriamente dita.

Além disso, como o cálculo utiliza diretamente o erro, alterações abruptas na referência (`target`) também podem produzir um grande termo derivativo.

Isso pode ser aceitável em alguns controladores, mas deve ser considerado durante a definição do comportamento esperado.

### Recomendação

A evolução do PID deve considerar:

* `dt`;
* comportamento na primeira amostra;
* mudanças abruptas de setpoint;
* eventualmente derivada da medição em casos onde isso seja desejável;
* filtragem do termo derivativo caso o ruído dos sensores exija.

Não implementar filtragem apenas por antecipação: ela deve ser introduzida quando houver necessidade observada.

---

# 9. AUD-008-07 — `reset()` é adequado e deve permanecer parte do contrato

**Severidade:** Informativo

A existência de:

```java
public void reset()
```

é importante.

O controlador mantém estado interno:

* integral;
* último erro;
* estado da primeira execução.

Portanto, reutilizar uma instância sem reset pode carregar estado de uma operação anterior.

O comportamento atual de:

```java
setPID(...)
```

também chama:

```java
reset();
```

Isso é coerente.

### Atenção futura

Caso o projeto passe a alterar ganhos durante uma operação, deve ser decidido explicitamente se alterar os ganhos deve ou não zerar o estado.

Não é necessariamente correto assumir que todo `setPID()` deve sempre resetar o controlador.

Por enquanto, o comportamento atual é aceitável.

---

# 10. AUD-008-08 — `control` e `commands` devem ter responsabilidades diferentes

**Severidade:** Arquitetural / Alta

A auditoria indica uma separação conceitual importante.

## `control`

Deve conter mecanismos matemáticos e de controle.

Exemplos:

* PID;
* controladores de velocidade;
* controladores de posição;
* filtros;
* rampas;
* funções de controle;
* utilitários relacionados ao controle de sistemas.

Essas classes devem ser, na medida do possível, independentes de hardware.

---

## `commands`

Deve representar **ações ou operações do robô**.

Exemplos futuros:

```text
IntakeCommand
ShootCommand
MoveToPositionCommand
AlignToAprilTagCommand
SetShooterVelocityCommand
```

Um Command deve coordenar ações de um ou mais Subsystems.

Ele não deve substituir o Subsystem.

---

# 11. AUD-008-09 — Relação esperada entre as camadas

A arquitetura desejada deve caminhar para algo próximo de:

```text
OpMode
   │
   ▼
Commands / sequência de ações
   │
   ▼
Subsystems
   │
   ├── Intake
   ├── Shooter
   ├── Drivetrain
   ├── Localization
   └── Vision
        │
        ▼
     Hardware

Control
   │
   ├── PIDController
   ├── outros controladores
   └── utilidades de controle
```

`Control` não precisa necessariamente ficar acima ou abaixo dos Subsystems como uma camada rígida.

Um Subsystem pode utilizar um controlador:

```text
Shooter
   │
   └── PIDController
```

enquanto um Command pode solicitar:

```text
ShootCommand
       │
       ▼
    Shooter
       │
       ▼
 PIDController
       │
       ▼
    Motor
```

Essa divisão mantém cada responsabilidade relativamente localizada.

---

# 12. AUD-008-10 — Commands não devem incorporar lógica de hardware

**Severidade:** Alta

Um Command futuro não deve fazer diretamente:

```java
hardwareMap.get(...)
```

nem manipular motores diretamente como regra geral.

Exemplo indesejado:

```java
motor.setPower(...);
servo.setPosition(...);
```

dentro de um Command.

A responsabilidade do Command deve ser expressar a operação:

```text
"acionar Intake"
"disparar"
"mover para posição"
"alinhar"
```

e delegar a implementação ao Subsystem.

Isso evita que a lógica operacional se espalhe pelos OpModes e Commands.

---

# 13. AUD-008-11 — Não há Scheduler/Executor de Commands

**Severidade:** Arquitetural / Alta

Com o código fornecido, não existe infraestrutura para executar Commands.

Ainda não está definido:

* como um Command começa;
* como é executado a cada ciclo;
* quando termina;
* como é cancelado;
* como ocorre concorrência;
* como conflitos entre Commands são tratados;
* como sequências são executadas;
* como timeouts funcionam.

Consequentemente, criar vários Commands agora sem definir o mecanismo de execução provavelmente produziria uma arquitetura inconsistente.

### Recomendação

Antes de criar uma quantidade significativa de Commands, definir um modelo mínimo de execução.

Não é necessário construir um framework complexo.

Um scheduler simples e adequado ao robô pode ser suficiente.

---

# 14. AUD-008-12 — Não adotar framework de Commands automaticamente

**Severidade:** Decisão arquitetural

A existência da pasta:

```text
commands/
```

não implica que o projeto precise reproduzir uma arquitetura completa de frameworks externos.

A decisão deve considerar:

* tamanho do projeto;
* quantidade de OpModes;
* quantidade de sequências;
* necessidade de concorrência;
* complexidade dos Subsystems;
* facilidade de depuração em competição;
* overhead introduzido pela abstração.

O objetivo deve ser obter uma separação clara de responsabilidades, e não maximizar a quantidade de abstrações.

---

# 15. AUD-008-13 — `PIDController` não deve conhecer o contexto do robô

**Severidade:** Informativo / Arquitetural

A implementação atual tem uma característica positiva:

```text
PIDController
```

não depende de:

```text
HardwareMap
DcMotor
DcMotorEx
Servo
IMU
Vision
Subsystem
OpMode
```

Isso deve ser preservado.

O controlador pode ser reutilizado para:

* velocidade do Shooter;
* posição de mecanismo;
* controle de trajetória;
* outros mecanismos que necessitem de realimentação.

A especialização deve ocorrer no consumidor, não no controlador.

---

# 16. Limitações da auditoria

Esta auditoria foi feita sobre o código fornecido para:

```text
commands/Command.java
control/PIDController.java
```

Portanto, não é possível concluir a partir desses arquivos isoladamente:

* quais Commands já são necessários;
* quais controladores adicionais são necessários;
* quais OpModes utilizam PID;
* quais ganhos estão sendo utilizados;
* quais limites físicos dos atuadores devem ser aplicados;
* se existe alguma infraestrutura adicional de execução fora dos arquivos apresentados.

Essas questões devem ser verificadas durante a integração das próximas auditorias.

---

# 17. Decisões arquiteturais recomendadas

## Decisão 1 — Manter `control` independente de hardware

**Recomendado.**

`PIDController` deve permanecer genérico.

---

## Decisão 2 — Commands coordenam Subsystems

**Recomendado.**

Commands não devem se tornar uma segunda implementação dos Subsystems.

---

## Decisão 3 — Não preencher `Command` antes de definir o ciclo de vida

**Recomendado.**

Definir primeiro:

```text
initialize()
execute()
isFinished()
end()
```

ou outro contrato mínimo equivalente.

---

## Decisão 4 — Introduzir `dt` no PID

**Recomendado.**

A implementação atual deve ser considerada um PID discreto simplificado.

Antes de utilizar o controlador como base para controle preciso, deve ser definida uma estratégia de tempo.

---

## Decisão 5 — Não implementar recursos avançados prematuramente

Não há necessidade imediata de adicionar:

* filtros complexos;
* feedforward;
* múltiplos modos de PID;
* cascatas;
* scheduler sofisticado;
* sistema completo de requirements;
* abstrações genéricas de sequência.

Esses recursos devem surgir de necessidades reais identificadas nos Subsystems e nos OpModes.

---

# 18. Plano de evolução

### Etapa 1 — Definir contrato de Command

Definir o ciclo de vida mínimo.

Exemplo conceitual:

```text
initialize()
execute()
isFinished()
end()
```

---

### Etapa 2 — Definir executor/scheduler mínimo

Responsável por:

```text
Command
    ↓
initialize()
    ↓
execute() repetidamente
    ↓
isFinished()
    ↓
end()
```

---

### Etapa 3 — Integrar Commands aos Subsystems

Exemplos:

```text
ShootCommand
    ↓
Shooter

IntakeCommand
    ↓
Intake

Drive/Move Command
    ↓
Drivetrain
```

---

### Etapa 4 — Revisar `PIDController`

Introduzir `dt` e testar o comportamento.

Depois avaliar:

* anti-windup;
* limites;
* derivada;
* tolerância;
* estabilidade;
* necessidade de feedforward.

---

### Etapa 5 — Migrar responsabilidades dos OpModes

Os OpModes devem progressivamente assumir o papel de composição/entrada:

```text
OpMode
    ↓
Command
    ↓
Subsystem
    ↓
Hardware
```

em vez de concentrar lógica operacional diretamente no OpMode.

---

# 19. Resultado da auditoria

| Componente                     | Estado                   | Avaliação                                 |
| ------------------------------ | ------------------------ | ----------------------------------------- |
| `PIDController`                | Implementado             | Base funcional, mas simplificada          |
| `Command`                      | Placeholder              | Arquitetura ainda não definida            |
| Scheduler                      | Não identificado         | Necessário antes de escalar Commands      |
| Lifecycle de Command           | Não definido             | Deve ser especificado                     |
| Integração Command → Subsystem | Não implementada         | Próxima etapa                             |
| PID com `dt`                   | Não implementado         | Necessário para controle temporal robusto |
| Anti-windup completo           | Não implementado         | Avaliar posteriormente                    |
| Limite de saída do PID         | Não implementado         | Decisão arquitetural pendente             |
| Dependência de hardware no PID | Não existe               | Ponto positivo                            |
| Separação Control/Subsystem    | Conceitualmente possível | Deve ser preservada                       |

---

# 20. Conclusão

A camada `control` já possui uma primeira implementação útil através do `PIDController`, mas o controlador deve ser tratado como uma **implementação PID discreta simplificada**, e não ainda como uma solução completa de controle temporal.

O principal trabalho desta auditoria não é adicionar funcionalidades ao PID imediatamente, mas estabelecer a separação de responsabilidades:

```text
Control
    → matemática e algoritmos de controle

Subsystem
    → estado e operação dos mecanismos

Command
    → coordenação de operações dos Subsystems

OpMode
    → composição/execução de alto nível
```

A classe `Command` vazia não deve ser preenchida arbitrariamente. Antes disso, o projeto deve definir um contrato mínimo de ciclo de vida e um mecanismo simples de execução.

O próximo passo natural é, portanto, **definir a arquitetura mínima de Commands/Scheduler e então conectar essa arquitetura aos Subsystems já auditados**, sem antecipar um framework excessivamente complexo.
