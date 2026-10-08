# Mecanismos da temporada BIOBUZZ: rampa de retenção e guarda-chuva

## Onde ficam e por quê
`seasons/biobuzz/` (camada Season). São mecanismos do jogo atual; o núcleo universal (Robot,
Subsystems, Localization, Vision) não os conhece. O Robot não foi alterado: a composição fica em
`BiobuzzRobot` (Robot universal + mecanismos da temporada). Na próxima temporada, o pacote inteiro é
substituído sem mexer no núcleo.

## Classes
- `BallRetainer` (servo): `block()`, `release()`, `getState()`, `isConfigured()`. Config: `BallRetainerConfig`.
- `Umbrella` (motor Core Hex, RUN_TO_POSITION): `moveToRest()`, `moveToForward()`, `isBusy()`, `stop()`,
  `isConfigured()`. Config: `UmbrellaConfig`.
- Nenhum nome do jogo (Flower, scoring) nas classes. Servo e motor não são expostos.

## Segurança enquanto não há calibração
Nenhum valor físico existia no projeto, então **nenhum foi inventado**: posições e potência são
`Double.NaN` (= não configurado). Sem calibração completa, `block/release/moveTo*` não fazem nada, o
`init()` do Umbrella não toca no motor e `ENABLED = false` impede até a criação dos mecanismos (evita erro
de dispositivo ausente no Hub). Verificado com hardware simulado: zero chamadas ao hardware.

## Estado
| Item | Estado |
|---|---|
| BallRetainer / Umbrella / BiobuzzRobot | IMPLEMENTADO (compila contra stubs) |
| Integração à composição | INTEGRADO via `BiobuzzRobot`; **nenhum OpMode o usa ainda** |
| Posições do servo, posições/potência/sentido do motor, nomes no Hub | CALIBRAÇÃO PENDENTE / TESTE FÍSICO PENDENTE |
| Command de disparo | DECISÃO PENDENTE (não criado) |

## Para ativar
1. Configurar os dispositivos no Driver Hub e ajustar os nomes nas Configs.
2. Medir e preencher as posições (servo 0–1; motor em ticks a partir do repouso no INIT), potência e sentido.
3. `ENABLED = true` em cada Config e usar `BiobuzzRobot` no OpMode (`getRobot()` devolve o Robot universal).

## Sequência de disparo proposta (NÃO implementada: depende de valores e de decisão)
Command da Season: shooter.setRPM → esperar `isReady` → `retainer.release()` → `umbrella.moveToForward()`
→ esperar `!isBusy()` → intake.pushOne → `umbrella.moveToRest()` → `retainer.block()`.
Os mecanismos não conhecem essa ordem; só o Command.

## Lacuna de contrato em `PoseObservation` (proposta, nada alterado)
1. `timestampMs` é o momento da leitura, não da captura. A Limelight informa latência de captura/
   processamento (`getCaptureLatency`/`getTargetingLatency`, presentes no sample); propõe-se
   `timestamp = agora - latência` na `Limelight`, sem mudar a classe.
2. Não há identificação do sistema de coordenadas de origem. Proposta: manter a regra atual (a fonte
   devolve null até estar alinhada) e só criar um campo/transformação quando houver o alinhamento medido.
3. Não há confiança/qualidade da observação. Proposta: adiar até existir um critério de validade medido.
