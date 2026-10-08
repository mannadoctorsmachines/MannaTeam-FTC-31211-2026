# Contratos: Localization, Vision e Robot (etapa 2 — 07/10/2026)

Estado: **IMPLEMENTADO**, compila contra stubs do FTC SDK. **NÃO VALIDADO no robô.**

## Arquitetura

```text
SEASON → OPMODES/STRATEGY → COMMANDS → ROBOT
                                         ├── Drivetrain, Shooter, Intake  (Subsystems)
                                         ├── Localizer  ── GobildaOdometry ── GoBildaPinpointDriver
                                         └── Vision     ── AprilTagCamera  ── VisionPortal/AprilTagProcessor
                                                        └─ Limelight       ── Limelight3A
```
Quem conhece cada SDK: Pinpoint só em `GobildaOdometry`/`GobildaOdometryConfig`; FTC Vision só em
`AprilTagCamera`; Limelight só em `Limelight`. Pedro Pathing não está no projeto e nada o referencia.

## Localization
- `Pose` (cm, cm, graus). Imutável, heading normalizado em [-180, 180].
  Convenção (documentação goBILDA, **a validar**): X+ frente, Y+ esquerda, heading+ anti-horário,
  origem = pose do `init()` (0,0,0).
- `Localizer`: `init`, `update`, `getPose`, `getXCm/getYCm/getHeadingDegrees`, `isPoseValid`,
  `resetPose`, `setPose`, `correctPose`.
  - Antes do `init()`: pose = ZERO, inválida. Válida após `init()` + 1º `update()` (ou `setPose`).
  - `init()` recalibra o giroscópio do Pinpoint: **robô parado**.
  - `resetPose()` = `setPose(ZERO)`; não recalibra o giroscópio.
  - `correctPose(PoseObservation)`: substitui a pose (sem fusão). **Desligado**
    (`LocalizationConfig.ACCEPT_VISION_CORRECTIONS = false`).
- Dono único do Pinpoint: `GobildaOdometry`. O `Drivetrain` não o inicializa mais.
- Config do Pinpoint (nome, offsets, pod, direções): `GobildaOdometryConfig`. Nome mantido: `"odmetry"`.
- Fronteira para Pedro (futura): converter `Pose` (cm/graus) para polegadas/radianos no adaptador de
  integração; Localization não depende de Pedro.

## Vision
- `Vision`: `init`, `update`, `hasTarget`, `getTargetId`, `hasRange`, `getRangeInches`,
  `getBearingDegrees`, `getYawDegrees`, `getForwardInches`, `getSideInches`, `getObservationAgeMs`,
  `getPoseObservation`, `close`. Mesmas unidades de antes (polegadas/graus); bearing+ = alvo à esquerda.
- `AprilTagCamera`: comportamento idêntico ao anterior; `getPoseObservation()` = null (a câmera não tem
  `setCameraPose` configurado, então `robotPose` não seria a pose do robô).
- `Limelight` (APIs do sample `SensorLimelight3A`): bearing = `-tx`; **sem range/yaw/forward/side**
  (`hasRange()` = false). Botpose só publicado se `LimelightConfig.PUBLISH_BOTPOSE` (padrão false).
- Escolha do provider: `RobotConfig.VISION_PROVIDER` (padrão `APRILTAG_CAMERA`), montado no `Robot`.

## Robot
- Ordem do `init`: Drivetrain → Localization → Shooter → Intake. Vision: `initVision()`.
- `update()` (1×/ciclo): Localization → Vision (→ `correctPose` se habilitado) → Intake.
- `stopAll()`, `closeVision()`. Getters: `getDrivetrain/getShooter/getIntake/getLocalization/getVision`.
- Flags: `USE_SHOOTER`, `USE_FEEDER`, `USE_LOCALIZATION` (novo, true), `USE_CAMERA`.

## Mudanças funcionais (explícitas)
1. **Pinpoint passa a ser configurado no INIT** (offsets, pod, direções, `resetPosAndIMU`). Antes só era
   obtido no Drivetrain e nunca configurado. Risco: nome do device errado quebra o INIT; robô em
   movimento no INIT corrompe o heading do Pinpoint. Nada no projeto lê a pose ainda.
2. **`robot.update()` substitui `camera.update()`/`intake.update()` nos OpModes** (TeleopTest2,
   CalibrarDistanciaRPM, NovoTeleop) e `sleep(20)` → `aguardarCiclo()` na `AutoBaseSimples`. A visão é
   atualizada no início do ciclo (antes: durante). Efeito esperado: nenhum além de leitura mais fresca.
3. Consumidores de range passam a exigir `hasRange()` (igual a `hasTarget()` na AprilTagCamera).

## Pendências / validação física
- Confirmar nome do Pinpoint no Hub (`odmetry` × `odometry`), offsets, pod e direções; testar heading,
  sinal de X/Y e reset empurrando o robô.
- Limelight: confirmar `DEVICE_NAME`/`PIPELINE_INDEX`; sinal do bearing; range exige API de pose do alvo
  no espaço da câmera (não consta nos samples do projeto).
- Alinhar o sistema de coordenadas Vision × Localization antes de habilitar `correctPose`.
- `AUTO_*` em `RConstants`, `ShooterLista` e `CalcDistAlvo` (util) são calibrações/estratégia da
  temporada ainda dentro do núcleo; mover é uma decisão pendente. `backups/` ainda usam `AprilTagCamera`
  direto (arquivo morto).
- Não tratados (fora de escopo): Viper, PID/`dt`, Pedro, Commands concretos, bugs do `NovoTeleop` e do
  `CalibrarDistanciaRPM` (registrados no doc anterior).
