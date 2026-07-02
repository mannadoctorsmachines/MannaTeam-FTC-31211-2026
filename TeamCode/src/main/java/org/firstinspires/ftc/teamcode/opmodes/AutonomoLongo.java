package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.core.RConstants;
import org.firstinspires.ftc.teamcode.drive.DriveSistema2;
import org.firstinspires.ftc.teamcode.mechanisms.CameraTurret;
import org.firstinspires.ftc.teamcode.mechanisms.IntakeSistema;
import org.firstinspires.ftc.teamcode.mechanisms.ShooterSistema;
import org.firstinspires.ftc.teamcode.util.CalcDistAlvo;
import org.firstinspires.ftc.teamcode.util.ShooterLista;
import org.firstinspires.ftc.teamcode.vision.AprilTagCamera;

@Autonomous(name = "DECODE Auto Completo", group = "Competition")
public class AutonomoLongo extends LinearOpMode {

    private DriveSistema2 drive;

    private AprilTagCamera camera;
    private CameraTurret turret;
    private ShooterSistema shooter;
    private IntakeSistema feeder;

    private CalcDistAlvo distanceCalculator;
    private ShooterLista shooterTable;

    @Override
    public void runOpMode() {
        initSystems();

        telemetry.addLine("Autônomo DECODE pronto.");
        telemetry.addData("Camera", RConstants.USE_CAMERA ? "Ativa" : "Desativada");
        telemetry.addData("Turret", RConstants.USE_TURRET ? "Ativa" : "Desativada");
        telemetry.addData("Shooter", RConstants.USE_SHOOTER ? "Ativo" : "Desativado");
        telemetry.addData("Feeder", RConstants.USE_FEEDER ? "Ativo" : "Desativado");
        telemetry.update();

        waitForStart();

        if (isStopRequested()) {
            stopAll();
            return;
        }

        driveForwardCm(10, 0.45, 1000);

        boolean foundTag = false;

        if (RConstants.USE_CAMERA && RConstants.USE_TURRET) {
            foundTag = searchForAprilTag(2500);
        }

        double targetRPM = RConstants.DEFAULT_SHOOTER_RPM;

        if (foundTag) {
            aimAtAprilTag(1800);

            double aprilTagRangeInches = camera.getRangeInches();
            double shooterDistanceCm =
                    distanceCalculator.getShooterDistanceCm(aprilTagRangeInches);

            targetRPM = shooterTable.getRPMForDistance(shooterDistanceCm);

            telemetry.addData("Auto distance cm", shooterDistanceCm);
            telemetry.addData("Auto target RPM", targetRPM);
            telemetry.update();
        }

        if (RConstants.USE_SHOOTER) {
            shooter.setRPM(targetRPM);
            waitForShooterReady(2500);

            if (RConstants.USE_FEEDER) {
                shootAmount(3, 700);
            } else {
                telemetry.addLine("Feeder desativado. Disparo ignorado.");
                telemetry.update();
                sleep(800);
            }
        } else {
            telemetry.addLine("Shooter desativado. Etapa de disparo ignorada.");
            telemetry.update();
            sleep(800);
        }

        stopAll();

        while (opModeIsActive()) {
            telemetry.addLine("Autônomo finalizado.");
            telemetry.update();
            sleep(50);
        }
    }

    private void initSystems() {
        drive = new DriveSistema2();
        drive.init(hardwareMap);

        distanceCalculator = new CalcDistAlvo();
        shooterTable = new ShooterLista();

        if (RConstants.USE_CAMERA) {
            camera = new AprilTagCamera();
            camera.init(hardwareMap);
        }

        if (RConstants.USE_TURRET) {
            turret = new CameraTurret();
            turret.init(hardwareMap);
        }

        if (RConstants.USE_SHOOTER) {
            shooter = new ShooterSistema();
            shooter.init(hardwareMap);
        }

        if (RConstants.USE_FEEDER) {
            feeder = new IntakeSistema();
            feeder.init(hardwareMap);
        }
    }

    private void driveForwardCm(double cm, double power, long timeoutMs) {
        drive.encoderDriveCm(cm, power);

        long startTime = System.currentTimeMillis();

        while (opModeIsActive()
                && drive.isBusy()
                && System.currentTimeMillis() - startTime < timeoutMs) {

            telemetry.addLine("Andando com encoder...");
            telemetry.addData("cm", cm);
            telemetry.update();

            sleep(20);
        }

        drive.stop();
    }

    private boolean searchForAprilTag(long timeoutMs) {
        if (camera == null || turret == null) {
            return false;
        }

        long startTime = System.currentTimeMillis();

        while (opModeIsActive()
                && System.currentTimeMillis() - startTime < timeoutMs) {

            camera.update();

            turret.updateTracking(
                    camera.hasTarget(),
                    camera.getBearingDegrees()
            );

            telemetry.addLine("Procurando AprilTag...");
            telemetry.addData("Has target", camera.hasTarget());
            telemetry.update();

            if (camera.hasTarget()) {
                return true;
            }

            sleep(20);
        }

        return false;
    }

    private void aimAtAprilTag(long timeoutMs) {
        if (camera == null || turret == null) {
            return;
        }

        long startTime = System.currentTimeMillis();

        while (opModeIsActive()
                && System.currentTimeMillis() - startTime < timeoutMs) {

            camera.update();

            turret.updateTracking(
                    camera.hasTarget(),
                    camera.getBearingDegrees()
            );

            boolean aimed =
                    camera.hasTarget()
                            && Math.abs(camera.getBearingDegrees())
                            <= RConstants.AIM_TOLERANCE_DEGREES;

            telemetry.addLine("Mirando na AprilTag...");
            telemetry.addData("Has target", camera.hasTarget());
            telemetry.addData("Bearing", camera.getBearingDegrees());
            telemetry.addData("Aimed", aimed);
            telemetry.update();

            if (aimed) {
                return;
            }

            sleep(20);
        }
    }

    private void waitForShooterReady(long timeoutMs) {
        if (shooter == null) {
            return;
        }

        long startTime = System.currentTimeMillis();

        while (opModeIsActive()
                && System.currentTimeMillis() - startTime < timeoutMs) {

            if (feeder != null) {
                feeder.update();
            }

            telemetry.addLine("Aguardando shooter estabilizar...");
            telemetry.addData("Target RPM", shooter.getTargetRPM());
            telemetry.addData("Current RPM", shooter.getCurrentRPM());
            telemetry.addData("Ready", shooter.isAtTargetRPM());
            telemetry.update();

            if (shooter.isAtTargetRPM()) {
                return;
            }

            sleep(20);
        }
    }

    private void shootAmount(int amount, long delayBetweenShotsMs) {
        if (feeder == null) {
            return;
        }

        for (int i = 0; i < amount && opModeIsActive(); i++) {
            feeder.pushOne();

            long startTime = System.currentTimeMillis();

            while (opModeIsActive()
                    && System.currentTimeMillis() - startTime < delayBetweenShotsMs) {

                feeder.update();

                telemetry.addData("Disparando", i + 1);
                telemetry.addData("de", amount);
                telemetry.update();

                sleep(20);
            }
        }
    }

    private void stopAll() {
        if (drive != null) {
            drive.stop();
        }

        if (feeder != null) {
            feeder.stop();
        }

        if (shooter != null) {
            shooter.stop();
        }

        if (turret != null) {
            turret.stop();
        }

        if (camera != null) {
            camera.close();
        }
    }
}