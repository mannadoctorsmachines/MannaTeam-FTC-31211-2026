package org.firstinspires.ftc.teamcode.backups;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.robot.RConstants;
import org.firstinspires.ftc.teamcode.subsystems.drivetrain.DrivetrainConfig;
import org.firstinspires.ftc.teamcode.subsystems.drivetrain.Drivetrain;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.shooter.Shooter;
import org.firstinspires.ftc.teamcode.util.CalcDistAlvo;
import org.firstinspires.ftc.teamcode.util.ShooterLista;
import org.firstinspires.ftc.teamcode.vision.AprilTagCamera;

@TeleOp(name = "TeleopTeste", group = "Competition unofficial")
public class TeleopTest extends OpMode {

    private Drivetrain drive;
    private AprilTagCamera camera;
    private Shooter shooter;
    private Intake intake;

    private CalcDistAlvo distanceCalculator;
    private ShooterLista shooterTable;

    private double lastShooterDistanceCm = 0.0;
    private double lastTargetRPM = RConstants.DEFAULT_SHOOTER_RPM;
    private boolean hasCameraShooterSolution = false;
    private long lastCameraSolutionTimeMs = 0;

    private boolean shooterSequenceActive = false;
    private long shooterStartTimeMs = 0;
    private boolean intakeRunning = false;

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
        telemetry.addLine("gamepad2 RB: intake independente");
        telemetry.addLine("gamepad2 LB: shooter; intake após 2 segundos");
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

        lastShooterDistanceCm = distanceCalculator.getShooterDistanceCm(
                camera.getRangeInches()
        );

        lastTargetRPM = shooterTable.getRPMForDistance(lastShooterDistanceCm);
        hasCameraShooterSolution = true;
        lastCameraSolutionTimeMs = System.currentTimeMillis();
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
//            axial = MathU.aplicarZonaNeutra(-gamepad1.left_stick_y, 0.55);
//            lateral = MathU.aplicarZonaNeutra(gamepad1.left_stick_x, 0.55) * 1.1;
//            yaw = MathU.aplicarZonaNeutra(gamepad1.right_stick_x, 0.55);

            axial = -gamepad1.left_stick_y;
            lateral = gamepad1.left_stick_x;
            yaw = gamepad1.right_stick_x;
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
        boolean shooterButton = gamepad2.left_bumper;
        boolean independentIntakeButton = gamepad2.right_bumper;

        if (emergencyStop) {
            shooterSequenceActive = false;

            if (shooter != null) {
                shooter.stop();
            }

            if (intake != null) {
                intake.stop();
            }

            intakeRunning = false;
            return;
        }

        if (shooter != null && shooterButton) {
            if (!shooterSequenceActive) {
                shooterSequenceActive = true;
                shooterStartTimeMs = System.currentTimeMillis();
            }

            // Se a câmera ainda não encontrou a tag, usa o RPM padrão.
            double requestedRPM = hasFreshCameraShooterSolution()
                    ? lastTargetRPM
                    : RConstants.DEFAULT_SHOOTER_RPM;

            shooter.setRPM(requestedRPM);
        } else {
            shooterSequenceActive = false;

            if (shooter != null) {
                shooter.stop();
            }
        }

        long shooterElapsedMs = shooterSequenceActive
                ? System.currentTimeMillis() - shooterStartTimeMs
                : 0;

        boolean delayedIntakeFromShooter = shooterSequenceActive
                && shooterElapsedMs >= RConstants.SHOOTER_SPINUP_DELAY_MS;

        // RB controla apenas o intake. LB controla o shooter e, após o atraso,
        // também libera o intake. Nenhum comando de RB liga o shooter.
        intakeRunning = independentIntakeButton || delayedIntakeFromShooter;

        if (intake != null) {
            intake.update();

            if (intakeRunning) {
                intake.startContinuous();
            } else if (intake.isContinuous() || intake.isBusy()) {
                intake.stopContinuous();
            }
        }
    }

    private void sendTelemetry() {
        telemetry.addLine("----- CONTROLES -----");
        telemetry.addData("RB intake", gamepad2.right_bumper);
        telemetry.addData("LB shooter", gamepad2.left_bumper);
        telemetry.addData("X alinhamento", alignmentRequested);

        telemetry.addLine("----- MIRA -----");
        telemetry.addData("AprilTag visível", camera != null && camera.hasTarget());
        telemetry.addData("Alinhado", alignedWithTag);
        telemetry.addData("Heading", drive.getHeadingDegrees());

        if (camera != null && camera.hasTarget()) {
            telemetry.addData("Tag ID", camera.getTargetId());
            telemetry.addData("Bearing", camera.getBearingDegrees());
            telemetry.addData("Range (in)", camera.getRangeInches());
        }

        telemetry.addLine("----- SHOOTER -----");
        telemetry.addData("Solução atual da câmera", hasFreshCameraShooterSolution());
        telemetry.addData("Distância ao gol (cm)", lastShooterDistanceCm);
        telemetry.addData("RPM alvo", lastTargetRPM);

        if (shooter != null) {
            long elapsedMs = shooterSequenceActive
                    ? System.currentTimeMillis() - shooterStartTimeMs
                    : 0;

            telemetry.addData("RPM atual", shooter.getCurrentRPM());
            telemetry.addData("Shooter pronto", shooter.isAtTargetRPM());
            telemetry.addData("Tempo acelerando (ms)", elapsedMs);
        }

        telemetry.addData("Intake ligado", intakeRunning);
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
