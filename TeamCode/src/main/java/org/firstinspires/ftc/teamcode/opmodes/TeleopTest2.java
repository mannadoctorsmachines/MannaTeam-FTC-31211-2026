package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.robot.RConstants;
import org.firstinspires.ftc.teamcode.subsystems.drivetrain.DrivetrainConfig;
import org.firstinspires.ftc.teamcode.subsystems.drivetrain.Drivetrain;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.shooter.Shooter;
import org.firstinspires.ftc.teamcode.util.CalcDistAlvo;
import org.firstinspires.ftc.teamcode.util.MathU;
import org.firstinspires.ftc.teamcode.util.ShooterLista;
import org.firstinspires.ftc.teamcode.vision.apriltag.AprilTagCamera;

@TeleOp(name = "Teste do teste", group = "Competition")
public class TeleopTest2 extends OpMode {

    private enum ShooterMode {
        OFF,
        MANUAL,
        AUTO
    }

    private Drivetrain drive;
    private AprilTagCamera camera;
    private Shooter shooter;
    private Intake intake;

    private CalcDistAlvo distanceCalculator;
    private ShooterLista shooterTable;

    private double lastRawCameraDistanceCm = 0.0;
    private double lastUnfilteredShooterDistanceCm = 0.0;
    private double lastShooterDistanceCm = 0.0;
    private double lastTargetRPM = RConstants.DEFAULT_SHOOTER_RPM;
    private boolean hasCameraShooterSolution = false;
    private long lastCameraSolutionTimeMs = 0;
    private int lastTargetId = -1;

    private boolean shooterSequenceActive = false;
    private long shooterStartTimeMs = 0;
    private ShooterMode shooterMode = ShooterMode.OFF;
    private double activeTargetRPM = 0.0;
    private boolean autoShooterWaitingForCamera = false;

    private boolean intakeRunning = false;
    private boolean intakeReversing = false;

    private boolean alignmentRequested = false;
    private boolean alignedWithTag = false;

    @Override
    public void init() {
        drive = new Drivetrain();
        drive.init(hardwareMap);

        distanceCalculator = new CalcDistAlvo();
        shooterTable = new ShooterLista();

        if (RConstants.USE_CAMERA) {
            camera = new AprilTagCamera();
            camera.init(hardwareMap);
        }

        if (RConstants.USE_SHOOTER) {
            shooter = new Shooter();
            shooter.init(hardwareMap);
        }

        if (RConstants.USE_FEEDER) {
            intake = new Intake();
            intake.init(hardwareMap);
        }

        telemetry.addLine("DECODE TeleOp pronto.");
        telemetry.addLine("gamepad2 LB: shooter manual");
        telemetry.addLine("gamepad2 LT: autoshooter pela câmera");
        telemetry.addLine("gamepad2 RB: intake para frente");
        telemetry.addLine("gamepad2 RT: intake reverso/destravar");
        telemetry.addLine("gamepad2 X: alinhar a frente com a AprilTag");
        telemetry.addLine("gamepad2 B: parada dos mecanismos");
        telemetry.update();
    }

    @Override
    public void loop() {
        updateCameraAndShooterSolution();
        updateDrive();
        updateMechanisms();
        sendTelemetry();
    }

    /**
     * No TeleOp, a webcam Logitech mede a distância pela AprilTag. A tabela
     * converte essa distância no RPM necessário para o shooter.
     */
    private void updateCameraAndShooterSolution() {
        if (camera == null) {
            return;
        }

        camera.update();

        if (!camera.hasTarget()) {
            return;
        }

        int currentTargetId = camera.getTargetId();
        boolean solutionExpired = !hasFreshCameraShooterSolution();
        boolean targetChanged = lastTargetId >= 0 && currentTargetId != lastTargetId;

        if (solutionExpired || targetChanged) {
            distanceCalculator.resetDistanceFilter();
        }

        double rangeInches = camera.getRangeInches();
        lastRawCameraDistanceCm = distanceCalculator.getCameraDistanceCm(rangeInches);
        lastUnfilteredShooterDistanceCm =
                distanceCalculator.getShooterDistanceCm(rangeInches);
        lastShooterDistanceCm =
                distanceCalculator.updateFilteredShooterDistanceCm(rangeInches);

        lastTargetRPM = shooterTable.getRPMForDistance(lastShooterDistanceCm);
        hasCameraShooterSolution = true;
        lastCameraSolutionTimeMs = System.currentTimeMillis();
        lastTargetId = currentTargetId;
    }

    private boolean hasFreshCameraShooterSolution() {
        return hasCameraShooterSolution
                && System.currentTimeMillis() - lastCameraSolutionTimeMs
                <= RConstants.CAMERA_SOLUTION_MAX_AGE_MS;
    }

    private void updateDrive() {
        double axial;
        double lateral;
        double yaw;

        if (gamepad1.dpad_up) {
            axial = 1.0;
            lateral = 0.0;
            yaw = 0.0;
        } else if (gamepad1.dpad_down) {
            axial = -1.0;
            lateral = 0.0;
            yaw = 0.0;
        } else if (gamepad1.dpad_left) {
            axial = 0.0;
            lateral = -1.0;
            yaw = 0.0;
        } else if (gamepad1.dpad_right) {
            axial = 0.0;
            lateral = 1.0;
            yaw = 0.0;
        } else {
            axial = MathU.aplicarZonaNeutra(-gamepad1.left_stick_y, 0.55);
            lateral = MathU.aplicarZonaNeutra(gamepad1.left_stick_x, 0.55) * 1.1;
            yaw = MathU.aplicarZonaNeutra(gamepad1.right_stick_x, 0.55);
        }

        double speedMultiplier = gamepad1.left_bumper
                ? DrivetrainConfig.DRIVE_POWER_NORMAL
                : DrivetrainConfig.DRIVE_POWER_TURBO;

        alignmentRequested = gamepad2.x;
        alignedWithTag = false;

        if (alignmentRequested) {
            // O botão X só pode girar o robô. Nenhuma potência de translação
            // é enviada enquanto o alinhamento estiver ativo.
            axial = 0.0;
            lateral = 0.0;
            yaw = 0.0;

            if (camera != null && camera.hasTarget()) {
                double bearingDegrees = camera.getBearingDegrees();
                alignedWithTag = Math.abs(bearingDegrees)
                        <= RConstants.AIM_TOLERANCE_DEGREES;

                if (!alignedWithTag) {
                    double targetHeadingDegrees =
                            drive.getHeadingDegrees() + bearingDegrees;

                    yaw = drive.getTurnPowerToHeading(targetHeadingDegrees);
                }
            }
        }

        drive.driveMecanum(axial, lateral, yaw, speedMultiplier);
    }

    private void updateMechanisms() {
        boolean emergencyStop = gamepad2.b;
        boolean manualShooterRequested = gamepad2.left_bumper;
        boolean autoShooterRequested = gamepad2.left_trigger
                > RConstants.GAMEPAD_TRIGGER_THRESHOLD;
        boolean forwardIntakeRequested = gamepad2.right_bumper;
        boolean reverseIntakeRequested = gamepad2.right_trigger
                > RConstants.GAMEPAD_TRIGGER_THRESHOLD;

        if (emergencyStop) {
            shooterSequenceActive = false;
            shooterMode = ShooterMode.OFF;
            activeTargetRPM = 0.0;
            autoShooterWaitingForCamera = false;

            if (shooter != null) {
                shooter.stop();
            }

            if (intake != null) {
                intake.stop();
            }

            intakeRunning = false;
            intakeReversing = false;
            return;
        }

        boolean hasAutoSolution = hasFreshCameraShooterSolution();
        autoShooterWaitingForCamera = autoShooterRequested && !hasAutoSolution;

        ShooterMode requestedShooterMode = ShooterMode.OFF;

        // O autoshooter tem prioridade quando LT e LB forem pressionados juntos.
        // Se LT estiver sem alvo, LB ainda pode assumir o modo manual.
        if (autoShooterRequested && hasAutoSolution) {
            requestedShooterMode = ShooterMode.AUTO;
        } else if (manualShooterRequested) {
            requestedShooterMode = ShooterMode.MANUAL;
        }

        updateShooter(requestedShooterMode);

        long shooterElapsedMs = shooterSequenceActive
                ? System.currentTimeMillis() - shooterStartTimeMs
                : 0;

        boolean delayedIntakeFromShooter = shooterSequenceActive
                && shooterElapsedMs >= RConstants.SHOOTER_SPINUP_DELAY_MS;

        // RT tem prioridade para permitir destravar o intake imediatamente.
        intakeReversing = reverseIntakeRequested;
        intakeRunning = reverseIntakeRequested
                || forwardIntakeRequested
                || delayedIntakeFromShooter;

        if (intake != null) {
            intake.update();

            if (intakeReversing) {
                intake.startReverseContinuous();
            } else if (intakeRunning) {
                intake.startContinuous();
            } else if (intake.isContinuous() || intake.isBusy()) {
                intake.stopContinuous();
            }
        }
    }

    private void updateShooter(ShooterMode requestedMode) {
        if (shooter == null) {
            shooterMode = ShooterMode.OFF;
            shooterSequenceActive = false;
            activeTargetRPM = 0.0;
            return;
        }

        // Reinicia os 2 segundos quando liga ou troca entre manual e automático.
        if (requestedMode != shooterMode) {
            shooterMode = requestedMode;

            if (shooterMode == ShooterMode.OFF) {
                shooterSequenceActive = false;
            } else {
                shooterSequenceActive = true;
                shooterStartTimeMs = System.currentTimeMillis();
            }
        }

        if (shooterMode == ShooterMode.MANUAL) {
            activeTargetRPM = RConstants.MANUAL_SHOOTER_RPM;
            shooter.setRPM(activeTargetRPM);
        } else if (shooterMode == ShooterMode.AUTO) {
            activeTargetRPM = lastTargetRPM;
            shooter.setRPM(activeTargetRPM);
        } else {
            activeTargetRPM = 0.0;
            shooter.stop();
        }
    }

    private void sendTelemetry() {
        telemetry.addLine("----- CONTROLES -----");
        telemetry.addData("LB shooter manual", gamepad2.left_bumper);
        telemetry.addData("LT autoshooter", gamepad2.left_trigger);
        telemetry.addData("RB intake frente", gamepad2.right_bumper);
        telemetry.addData("RT intake reverso", gamepad2.right_trigger);
        telemetry.addData("X alinhamento", alignmentRequested);

        telemetry.addLine("----- MIRA -----");
        telemetry.addData("AprilTag visível", camera != null && camera.hasTarget());
        telemetry.addData("Alinhado", alignedWithTag);
        telemetry.addData("Heading", drive.getHeadingDegrees());

        if (camera != null && camera.hasTarget()) {
            telemetry.addData("Tag ID", camera.getTargetId());
            telemetry.addData("Bearing", camera.getBearingDegrees());
            telemetry.addData("Range (in)", camera.getRangeInches());
            telemetry.addData("Range cru (cm)", camera.getRangeInches() * 2.54);

            telemetry.addData("Y frontal (cm)", camera.getForwardInches() * 2.54);
            telemetry.addData("X lateral (cm)", camera.getSideInches() * 2.54);
        }

        telemetry.addLine("----- SHOOTER -----");
        telemetry.addData("Modo", shooterMode);
        telemetry.addData("Solução atual da câmera", hasFreshCameraShooterSolution());
        telemetry.addData("Auto aguardando câmera", autoShooterWaitingForCamera);
        telemetry.addData("Distância câmera crua (cm)", lastRawCameraDistanceCm);
        telemetry.addData("Distância corrigida sem filtro (cm)",
                lastUnfilteredShooterDistanceCm);
        telemetry.addData("Distância usada pelo auto (cm)", lastShooterDistanceCm);
        telemetry.addData("Distância dentro da tabela",
                shooterTable.isDistanceInsideTable(lastShooterDistanceCm));
        telemetry.addData("RPM calculado pela câmera", lastTargetRPM);
        telemetry.addData("RPM aplicado", activeTargetRPM);

        if (hasFreshCameraShooterSolution()
                && !shooterTable.isDistanceInsideTable(lastShooterDistanceCm)) {
            telemetry.addLine("ATENÇÃO: distância fora da tabela; usando o limite mais próximo.");
        }

        if (shooter != null) {
            long elapsedMs = shooterSequenceActive
                    ? System.currentTimeMillis() - shooterStartTimeMs
                    : 0;

            telemetry.addData("RPM atual", shooter.getCurrentRPM());
            telemetry.addData("Shooter pronto", shooter.isAtTargetRPM());
            telemetry.addData("Tempo acelerando (ms)", elapsedMs);
        }

        telemetry.addData("Intake ligado", intakeRunning);
        telemetry.addData("Intake reverso", intakeReversing);
        telemetry.update();
    }

    @Override
    public void stop() {
        if (drive != null) {
            drive.stop();
        }

        if (shooter != null) {
            shooter.stop();
        }

        if (intake != null) {
            intake.stop();
        }

        if (camera != null) {
            camera.close();
        }
    }
}
