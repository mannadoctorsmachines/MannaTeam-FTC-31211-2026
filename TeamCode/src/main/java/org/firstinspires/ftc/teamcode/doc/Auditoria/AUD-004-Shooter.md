# AUD-004 — Auditoria do Shooter

**Projeto:** FTC Team 31211 — 2026  
**Subsystem:** `org.firstinspires.ftc.teamcode.subsystems.shooter`  
**Arquivos analisados:** `Shooter.java`, `ShooterConfig.java`, `RConstants.java`  
**Status:** CONCLUÍDO  
**Resultado:** Sem bloqueador estrutural identificado

## 1. Objetivo

Avaliar a implementação do subsystem `Shooter`, sua configuração em `RConstants` e a coerência entre comando de RPM, leitura de encoder, limites operacionais e condição de prontidão.

## 2. Resumo executivo

O `Shooter` apresenta estrutura adequada para controle de um mecanismo de lançamento por velocidade. Utiliza `DcMotorEx`, `setVelocity()`, encoder, conversão entre ticks/s e RPM do rolo, limites de RPM e verificação independente da velocidade dos dois motores.

A conversão configurada é matematicamente consistente:

- `SHOOTER_MOTOR_TICKS_PER_REV = 537.7`
- `SHOOTER_MOTOR_REVS_PER_ROLLER_REV = 1.0`
- `SHOOTER_TICKS_PER_ROLLER_REV = 537.7`

O limite operacional configurado é `3000 RPM`, enquanto o RPM nominal configurado do motor é `6000 RPM`, com relação `1:1`.

Existe o valor:

- `MANUAL_SHOOTER_RPM = 20.0`
- `MIN_SHOOTER_RPM = 300.0`

Assim, a API efetivamente limita um comando de `20 RPM` para `300 RPM`. A equipe informou que esse valor de 20 RPM foi desenvolvido em campo durante uma operação emergencial para resolver um problema local e momentâneo. Portanto, isso não é classificado como defeito estrutural do Shooter.

**Conclusão:** o Shooter pode permanecer na arquitetura atual. Não foi identificado bloqueador estrutural que justifique reescrita do subsystem.

## 3. Estrutura analisada

### Inicialização

Os dois motores são obtidos pelo `HardwareMap` usando `SHOOTER_LEFT` e `SHOOTER_RIGHT`. As direções são configuradas individualmente e os encoders são resetados antes de `RUN_USING_ENCODER`.

### Controle por RPM

`setRPM()` valida a conversão, valida o RPM nominal, calcula o limite máximo, aplica `clamp()`, converte RPM para ticks/s e aplica `setVelocity()` aos dois motores.

### Controle por potência

`setRawPower()` permanece disponível para controle direto. Com `MIN_MOTOR_POWER = -1.0` e `MAX_MOTOR_POWER = 1.0`, todo o intervalo de potência está disponível.

### Parada

`stop()` zera o alvo e interrompe os motores por `setVelocity(0.0)` e `setPower(0.0)`.

### Feedback

O subsystem fornece RPM dos dois rolos, RPM média, RPM dos motores, ticks/s, posição dos encoders e RPM alvo.

## 4. Conversão de velocidade

A configuração atual define:

```text
SHOOTER_MOTOR_TICKS_PER_REV = 537.7
SHOOTER_MOTOR_REVS_PER_ROLLER_REV = 1.0
SHOOTER_TICKS_PER_ROLLER_REV = 537.7
```

O comando utiliza `SHOOTER_TICKS_PER_ROLLER_REV`, enquanto a leitura utiliza os parâmetros do motor e da transmissão.

Com os valores atuais, as duas representações são equivalentes.

**Resultado: APROVADO.**

## 5. Limites de RPM

Configuração:

```text
MIN_SHOOTER_RPM = 300
MAX_SHOOTER_RPM = 3000
SHOOTER_MOTOR_NOMINAL_RPM = 6000
SHOOTER_MOTOR_REVS_PER_ROLLER_REV = 1.0
```

O nominal do rolo é:

```text
6000 / 1.0 = 6000 RPM
```

Logo:

```text
maximumAllowedRPM = min(3000, 6000) = 3000 RPM
```

O limite de software está abaixo do valor nominal configurado.

**Resultado: APROVADO**, condicionado à confirmação dos dados do motor físico antes da calibração final.

## 6. Condição de prontidão

`isAtTargetRPM()` exige que os dois motores estejam dentro de `SHOOTER_READY_TOLERANCE_RPM = 85 RPM` em relação ao alvo.

A condição não utiliza apenas a média, evitando falso positivo quando um motor está acima e o outro abaixo do alvo.

**Resultado: APROVADO.**

## 7. RPM manual emergencial

`MANUAL_SHOOTER_RPM = 20.0` está abaixo de `MIN_SHOOTER_RPM = 300.0`.

Consequentemente, `setRPM(20.0)` resulta em `300 RPM`.

A equipe informou que os 20 RPM foram introduzidos em campo como operação emergencial para resolver um problema local e momentâneo.

Por isso:

- não será classificado como bug do subsystem;
- não será alterado arbitrariamente nesta auditoria;
- fica registrado como configuração operacional/histórica;
- deve ser revisitado antes da próxima calibração formal.

**Resultado: observação de configuração, não bloqueador estrutural.**

## 8. Spin-up e integração com alimentação

`SHOOTER_SPINUP_DELAY_MS = 2000` estabelece tempo mínimo de aceleração antes da liberação do intake. O Shooter fornece `isAtTargetRPM()` para a camada superior decidir quando liberar a alimentação.

Essa separação mantém a responsabilidade de velocidade no subsystem e a sequência operacional fora dele.

## 9. ShooterConfig

`ShooterConfig` está atualmente vazio.

Isso não constitui defeito funcional. Não é recomendada migração prematura dos parâmetros de `RConstants` para essa classe sem uma decisão arquitetural mais ampla.

## 10. Achados

| ID | Achado | Severidade | Status |
|---|---|---|---|
| AUD-004.1 | `MANUAL_SHOOTER_RPM = 20` está abaixo do mínimo de `300 RPM` | Baixa / configuração | Registrado |
| AUD-004.2 | Parâmetros do motor precisam de confirmação antes da calibração final | Média | Pendente de validação física |
| AUD-004.3 | `ShooterConfig` está vazio | Informativo | Sem ação necessária |
| AUD-004.4 | Conversão motor/rolo é consistente com os valores atuais | — | Aprovado |
| AUD-004.5 | `isAtTargetRPM()` verifica os dois motores individualmente | — | Aprovado |

## 11. Recomendações

Antes da calibração final, confirmar no motor físico instalado:

- `537.7 ticks/rev`;
- `6000 RPM nominal`;
- relação mecânica do motor para o rolo.

Antes de uma nova calibração de campo, revisar `MANUAL_SHOOTER_RPM = 20.0` e decidir se o valor ainda representa alguma necessidade operacional. Não alterar esse valor automaticamente apenas para satisfazer `MIN_SHOOTER_RPM`.

Não é recomendado neste momento:

- reescrever o controle de velocidade;
- introduzir PID próprio no subsystem;
- duplicar parâmetros em `ShooterConfig`;
- remover `setRawPower()`;
- substituir a condição de prontidão atual.

## 12. Conclusão

O subsystem `Shooter` está **aprovado estruturalmente para a próxima etapa do projeto**.

A implementação apresenta controle por velocidade, feedback por encoder, conversão consistente, limites operacionais, condição de prontidão bilateral, parada explícita e telemetria suficiente para calibração.

O `MANUAL_SHOOTER_RPM = 20.0` permanece documentado como configuração emergencial de campo e não como defeito estrutural.

**AUD-004 — CONCLUÍDO.**
