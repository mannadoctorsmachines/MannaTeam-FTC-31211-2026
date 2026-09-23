package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.robot.RConstants;
import org.firstinspires.ftc.teamcode.subsystems.drivetrain.DrivetrainConfig;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.shooter.Shooter;
import org.firstinspires.ftc.teamcode.util.CalcDistAlvo;
import org.firstinspires.ftc.teamcode.util.MathU;
import org.firstinspires.ftc.teamcode.util.ShooterLista;
import org.firstinspires.ftc.teamcode.vision.AprilTagCamera;
import org.firstinspires.ftc.teamcode.subsystems.drivetrain.Drivetrain;

/**
 * OpMode de bancada para descobrir a relação entre distância e RPM sem
 * recompilar o aplicativo a cada tentativa. Ele não controla o drive.
 */
@TeleOp(name = "DECODE Calibrar Distancia e RPM", group = "Calibration")
public class CalibrarDistanciaRPM extends OpMode {

    private AprilTagCamera camera;
    private Shooter shooter;
    private Intake intake;
    private CalcDistAlvo distanceCalculator;
    private ShooterLista shooterTable;
    private Drivetrain drive;

    private double selectedRPM = RConstants.MANUAL_SHOOTER_RPM;
    private double rawCameraDistanceCm = 0.0;
    private double unfilteredShooterDistanceCm = 0.0;
    private double filteredShooterDistanceCm = 0.0;
    private double tableSuggestedRPM = RConstants.DEFAULT_SHOOTER_RPM;

    private boolean hasCameraSolution = false;
    private long lastCameraSolutionTimeMs = 0;
    private int lastTargetId = -1;

    private boolean shooterRunning = false;
    private long shooterStartTimeMs = 0;
    private String statusMessage = "Pronto para calibrar.";

    private boolean previousDpadUp = false;
    private boolean previousDpadDown = false;
    private boolean previousDpadLeft = false;
    private boolean previousDpadRight = false;
    private boolean previousA = false;
    private boolean previousY = false;
    private boolean previousX = false;

    private boolean alignmentRequested = false;
    private boolean alignedWithTag = false;

    @Override
    public void init() {
        distanceCalculator = new CalcDistAlvo();
        shooterTable = new ShooterLista();
        drive = new Drivetrain();
        drive.init(hardwareMap);

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

        telemetry.addLine("Calibração pronta. O drive não será acionado.");
        telemetry.addLine("Use o gamepad2 e mantenha o robô parado.");
        telemetry.update();
    }

    @Override
    public void loop() {
        updateDrive();
        updateCamera();
        updateSelectedRPM();
        updateShooterAndIntake();
        sendTelemetry();
        rememberButtons();
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

    private void updateCamera() {
        if (camera == null) {
            return;
        }

        camera.update();

        if (!camera.hasTarget()) {
            return;
        }

        int currentTargetId = camera.getTargetId();
        boolean solutionExpired = !hasFreshCameraSolution();
        boolean targetChanged = lastTargetId >= 0 && currentTargetId != lastTargetId;

        if (solutionExpired || targetChanged) {
            distanceCalculator.resetDistanceFilter();
        }

        double rangeInches = camera.getRangeInches();
        rawCameraDistanceCm = distanceCalculator.getCameraDistanceCm(rangeInches);
        unfilteredShooterDistanceCm =
                distanceCalculator.getShooterDistanceCm(rangeInches);
        filteredShooterDistanceCm =
                distanceCalculator.updateFilteredShooterDistanceCm(rangeInches);
        tableSuggestedRPM =
                shooterTable.getRPMForDistance(filteredShooterDistanceCm);

        hasCameraSolution = true;
        lastCameraSolutionTimeMs = System.currentTimeMillis();
        lastTargetId = currentTargetId;
    }

    private boolean hasFreshCameraSolution() {
        return hasCameraSolution
                && System.currentTimeMillis() - lastCameraSolutionTimeMs
                <= RConstants.CAMERA_SOLUTION_MAX_AGE_MS;
    }

    private void updateSelectedRPM() {
        if (gamepad2.dpad_up && !previousDpadUp) {
            changeSelectedRPM(RConstants.SHOOTER_CALIBRATION_COARSE_STEP_RPM);
        }

        if (gamepad2.dpad_down && !previousDpadDown) {
            changeSelectedRPM(-RConstants.SHOOTER_CALIBRATION_COARSE_STEP_RPM);
        }

        if (gamepad2.dpad_right && !previousDpadRight) {
            changeSelectedRPM(RConstants.SHOOTER_CALIBRATION_FINE_STEP_RPM);
        }

        if (gamepad2.dpad_left && !previousDpadLeft) {
            changeSelectedRPM(-RConstants.SHOOTER_CALIBRATION_FINE_STEP_RPM);
        }

        if (gamepad2.y && !previousY) {
            if (hasFreshCameraSolution()) {
                selectedRPM = clampRPM(tableSuggestedRPM);
                statusMessage = "RPM sugerido pela tabela carregado.";
            } else {
                statusMessage = "Y ignorado: nenhuma AprilTag atual.";
            }
        }

        // Permite medir fisicamente quantos ticks aparecem quando o rolo é
        // girado manualmente. Só zera com o shooter parado.
        if (gamepad2.x && !previousX) {
            if (shooter == null) {
                statusMessage = "X ignorado: shooter não inicializado.";
            } else if (shooterRunning) {
                statusMessage = "Solte LB e espere o shooter parar antes de zerar.";
            } else {
                shooter.resetEncoders();
                statusMessage = "Encoders do shooter zerados.";
            }
        }
    }

    private void changeSelectedRPM(double changeRPM) {
        selectedRPM = clampRPM(selectedRPM + changeRPM);
        statusMessage = "RPM de teste alterado.";
    }

    private double clampRPM(double rpm) {
        return MathU.clamp(
                rpm,
                RConstants.MIN_SHOOTER_RPM,
                RConstants.MAX_SHOOTER_RPM
        );
    }

    private void updateShooterAndIntake() {
        boolean emergencyStop = gamepad2.b;
        boolean shooterRequested = gamepad2.left_bumper;
        boolean forwardIntakeRequested = gamepad2.right_bumper;
        boolean reverseIntakeRequested = gamepad2.right_trigger
                > RConstants.GAMEPAD_TRIGGER_THRESHOLD;
        boolean singleShotRequested = gamepad2.a && !previousA;

        if (intake != null) {
            intake.update();
        }

        if (emergencyStop) {
            stopMechanisms();
            statusMessage = "Mecanismos parados pelo botão B.";
            return;
        }

        if (shooter != null) {
            if (shooterRequested) {
                if (!shooterRunning) {
                    shooterRunning = true;
                    shooterStartTimeMs = System.currentTimeMillis();
                }

                shooter.setRPM(selectedRPM);
            } else {
                shooter.stop();
                shooterRunning = false;
            }
        }

        if (intake == null) {
            return;
        }

        if (reverseIntakeRequested) {
            intake.startReverseContinuous();
            return;
        }

        if (forwardIntakeRequested) {
            intake.startContinuous();
            return;
        }

        if (intake.isContinuous()) {
            intake.stopContinuous();
        }

        if (singleShotRequested) {
            if (!isMinimumSpinupComplete()) {
                statusMessage = "Aguarde 2 s com LB antes de alimentar a bola.";
            } else {
                intake.pushOne();
                statusMessage = String.format(
                        "Teste: %.1f cm e %.0f RPM.",
                        filteredShooterDistanceCm,
                        selectedRPM
                );
            }
        }
    }

    private boolean isMinimumSpinupComplete() {
        return shooterRunning
                && System.currentTimeMillis() - shooterStartTimeMs
                >= RConstants.SHOOTER_SPINUP_DELAY_MS;
    }

    private void sendTelemetry() {
        telemetry.addLine("----- AJUSTE DO RPM -----");
        telemetry.addData("D-pad cima/baixo", "+/- %.0f RPM",
                RConstants.SHOOTER_CALIBRATION_COARSE_STEP_RPM);
        telemetry.addData("D-pad direita/esquerda", "+/- %.0f RPM",
                RConstants.SHOOTER_CALIBRATION_FINE_STEP_RPM);
        telemetry.addData("RPM selecionado DO ROLO", selectedRPM);
        telemetry.addData("Y carrega RPM da tabela", tableSuggestedRPM);

        telemetry.addLine("----- TESTE -----");
        telemetry.addData("LB", "segurar para ligar shooter");
        telemetry.addData("A", "alimentar uma bola após 2 s");
        telemetry.addData("RB", "intake contínuo");
        telemetry.addData("RT", "intake reverso");
        telemetry.addData("B", "parar mecanismos");
        telemetry.addData("X", "zerar encoders do shooter parado");

        telemetry.addLine("----- CÂMERA E DISTÂNCIA -----");
        telemetry.addData("AprilTag atual", hasFreshCameraSolution());

        if (camera != null && camera.hasTarget()) {
            telemetry.addData("Tag ID", camera.getTargetId());
            telemetry.addData("Range da câmera (in)", camera.getRangeInches());
        }

        telemetry.addData("Distância câmera crua (cm)", rawCameraDistanceCm);
        telemetry.addData("Distância corrigida sem filtro (cm)",
                unfilteredShooterDistanceCm);
        telemetry.addData("Distância para anotar (cm)", filteredShooterDistanceCm);
        telemetry.addData("Distância dentro da tabela",
                shooterTable.isDistanceInsideTable(filteredShooterDistanceCm));
        telemetry.addData("RPM sugerido pela tabela", tableSuggestedRPM);

        if (hasFreshCameraSolution()
                && !shooterTable.isDistanceInsideTable(filteredShooterDistanceCm)) {
            telemetry.addLine("ATENÇÃO: sugestão limitada ao último ponto da tabela.");
        }

        telemetry.addLine("----- SHOOTER -----");
        telemetry.addData("Ligado", shooterRunning);
        telemetry.addData("Tempo mínimo concluído", isMinimumSpinupComplete());

        if (shooter != null) {
            telemetry.addData("Ticks/volta do motor",
                    RConstants.SHOOTER_MOTOR_TICKS_PER_REV);
            telemetry.addData("RPM nominal do motor",
                    RConstants.SHOOTER_MOTOR_NOMINAL_RPM);
            telemetry.addData("Voltas motor / volta rolo",
                    RConstants.SHOOTER_MOTOR_REVS_PER_ROLLER_REV);
            telemetry.addData("Ticks/volta do rolo",
                    RConstants.SHOOTER_TICKS_PER_ROLLER_REV);
            telemetry.addData("RPM nominal calculado do rolo",
                    shooter.getNominalRollerRPM());
            telemetry.addData("RPM máximo aceito pelo código",
                    shooter.getMaximumAllowedRollerRPM());

            if (!shooter.hasNominalMotorRPMConfigured()) {
                telemetry.addLine(
                        "ATENÇÃO: preencha SHOOTER_MOTOR_NOMINAL_RPM."
                );
            }

            telemetry.addData("Posição encoder esquerdo",
                    shooter.getLeftEncoderPosition());
            telemetry.addData("Posição encoder direito",
                    shooter.getRightEncoderPosition());
            telemetry.addData("Ticks/s esquerdo",
                    shooter.getLeftTicksPerSecond());
            telemetry.addData("Ticks/s direito",
                    shooter.getRightTicksPerSecond());
            telemetry.addData("RPM MOTOR esquerdo",
                    shooter.getLeftMotorRPM());
            telemetry.addData("RPM MOTOR direito",
                    shooter.getRightMotorRPM());
            telemetry.addData("RPM ROLO esquerdo", shooter.getLeftRPM());
            telemetry.addData("RPM ROLO direito", shooter.getRightRPM());
            telemetry.addData("Dentro da tolerância", shooter.isAtTargetRPM());
        }

        if (hasFreshCameraSolution()) {
            telemetry.addData(
                    "COPIAR PARA ShooterLista",
                    "{%.1f, %.0f}",
                    filteredShooterDistanceCm,
                    selectedRPM
            );
        }

        telemetry.addData("Status", statusMessage);
        telemetry.update();
    }

    private void rememberButtons() {
        previousDpadUp = gamepad2.dpad_up;
        previousDpadDown = gamepad2.dpad_down;
        previousDpadLeft = gamepad2.dpad_left;
        previousDpadRight = gamepad2.dpad_right;
        previousA = gamepad2.a;
        previousY = gamepad2.y;
        previousX = gamepad2.x;
    }

    private void stopMechanisms() {
        if (shooter != null) {
            shooter.stop();
        }

        if (intake != null) {
            intake.stop();
        }

        shooterRunning = false;
    }

    @Override
    public void stop() {
        stopMechanisms();

        if (camera != null) {
            camera.close();
        }
    }
}
