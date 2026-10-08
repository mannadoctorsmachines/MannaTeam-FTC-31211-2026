# Refatoração de 07/10/2026 — registro de mudanças e pendências

Estado: **IMPLEMENTADO** (compila contra stubs do FTC SDK). **NÃO VALIDADO** no robô.

## O que mudou

### Configuração com dono (D04/D05)
- `RConstants` ficou só com o que é geral: flags `USE_*`, limites de potência,
  limiar de gatilho, conversão polegada→cm e tempos/limites dos autônomos.
- `ShooterConfig`: passou a ter também `MANUAL_SHOOTER_RPM`, `DEFAULT_SHOOTER_RPM`,
  `SHOOTER_SPINUP_DELAY_MS` e passos de calibração. É a única fonte dos
  parâmetros físicos do shooter (Shooter, ShooterLista e Calibrar leem daqui).
- `IntakeConfig`: nome do motor, potências, tempo do pushOne.
- `DrivetrainConfig`: nome da IMU, nome do Pinpoint, PID de giro e tolerância.
- `VisionConfig` (novo): webcam, ID da tag, idade da leitura, tolerância de mira
  e constantes do cálculo de distância.
- Valores copiados sem alteração. Constantes duplicadas e sem consumidor foram
  removidas do `RConstants`.

### Robot (D02/D03)
- `Robot` compõe Drivetrain, Shooter e Intake; `init(hardwareMap)` os inicializa.
- Shooter e Intake respeitam `USE_SHOOTER`/`USE_FEEDER` (getter devolve `null`
  se desativado, como antes nos OpModes).
- Câmera é opcional: `initCamera(hardwareMap)` (respeita `USE_CAMERA`).
- `stopAll()` e `closeCamera()` substituem a lógica repetida de parada.
- O Robot **não** chama `update()`: o OpMode continua atualizando Intake e câmera.

### Commands (D10)
- `Command` (initialize / execute / isFinished / end(interrupted) / timeout).
- `CommandRunner`: executa em sequência, com timeout e cancelamento. Sem
  paralelismo/grupos por enquanto. Nenhum Command concreto foi criado.

### OpModes
- `TeleopTest2`, `AutoBaseSimples` (e todos os autônomos que herdam dele),
  `CalibrarDistanciaRPM` e `NovoTeleop` obtêm os subsistemas pelo `Robot`.
- `NovoTeleop`: removido `odo.init(...)` (campo nunca instanciado → NPE no INIT).
- `CalibrarDistanciaRPM.stop()` agora também para o drivetrain.

## Pendências que dependem de decisão ou do robô
1. Ticks/rev do shooter: `ShooterConfig`=28, antigo `RConstants`=537.7. Confirmar o motor.
2. `MANUAL_SHOOTER_RPM = 20` é limitado pelo Shooter a `MIN_SHOOTER_RPM` (1000).
3. Nome do Pinpoint: resolvido na etapa 2 (fonte única `GobildaOdometryConfig`, valor `"odmetry"`; confirmar no Hub).
4. `CalibrarDistanciaRPM` diz que não aciona o drive, mas aciona (gamepad1).
5. `NovoTeleop`: `ligado = !ligado` a cada ciclo e laços bloqueantes dentro do `loop()` com B.
6. Ciclo de atualização central (Intake.update), Localization (Pose/unidades),
   Pedro Pathing, PID com `dt`, Viper: **não implementados** (exigem decisão/spec/teste físico).
