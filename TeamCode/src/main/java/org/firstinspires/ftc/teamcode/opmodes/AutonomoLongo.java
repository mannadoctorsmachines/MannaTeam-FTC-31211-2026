package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import org.firstinspires.ftc.teamcode.core.RConstants;
import org.firstinspires.ftc.teamcode.drive.DriveSistema;
import org.firstinspires.ftc.teamcode.mechanisms.CameraTurret;
import org.firstinspires.ftc.teamcode.mechanisms.IntakeSistema;
import org.firstinspires.ftc.teamcode.mechanisms.ShooterSistema;
import org.firstinspires.ftc.teamcode.util.ShooterLista;
import org.firstinspires.ftc.teamcode.util.CalcDistAlvo;
import org.firstinspires.ftc.teamcode.vision.AprilTagCamera;

@Autonomous(name = "DECODE Auto Completo", group = "Competition")
public class AutonomoLongo extends LinearOpMode {

    private DriveSistema drive;
    private AprilTagCamera camera;
    private CameraTurret turret;
    private ShooterSistema shooter;
    private IntakeSistema feeder;

    private CalcDistAlvo distanceCalculator;
    private ShooterLista shooterTable;

    @Override
    public void runOpMode() {
        drive = new DriveSistema();
        camera = new AprilTagCamera();
        turret = new CameraTurret();
        shooter = new ShooterSistema();
        feeder = new IntakeSistema();

        distanceCalculator = new CalcDistAlvo();
        shooterTable = new ShooterLista();

        drive.init(hardwareMap);
        camera.init(hardwareMap);
        turret.init(hardwareMap);
        shooter.init(hardwareMap);
        feeder.init(hardwareMap);

        telemetry.addLine("Autônomo DECODE pronto.");
        telemetry.update();

        waitForStart();

        if (isStopRequested()) {
            return;
        }

        //sair do ponto inicial(ajustar)

        driveForwardCm(45.0, 0.45, 2500);

        //buscar um apriltag

        boolean foundTag = searchForAprilTag(2500);

        //se achou a tag, mira e calcula rpm. Se não achou, usa rpm padrão

        double targetRPM = RConstants.DEFAULT_SHOOTER_RPM;

        if (foundTag) {
            aimAtAprilTag(1800);

            double aprilTagRangeInches = camera.getRangeInches();
            double shooterDistanceCm = distanceCalculator.getShooterDistanceCm(aprilTagRangeInches);

            targetRPM = shooterTable.getRPMForDistance(shooterDistanceCm);

            telemetry.addData("Auto distance cm", shooterDistanceCm);
            telemetry.addData("Auto target RPM", targetRPM);
            telemetry.update();
        }

        //ligar shooter e esperar estabilizar

        shooter.setRPM(targetRPM);
        waitForShooterReady(2500);

        // disparar 3 vezes

        shootAmount(3, 700);

        // parar

        shooter.stop();
        turret.stop();
        drive.stop();
        camera.close();

        while (opModeIsActive()) {
            telemetry.addLine("Autônomo finalizado.");
            telemetry.update();
            sleep(50);
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
        }

        drive.stop();
    }

    private boolean searchForAprilTag(long timeoutMs) {
        long startTime = System.currentTimeMillis();

        while (opModeIsActive()
                && System.currentTimeMillis() - startTime < timeoutMs) {

            camera.update();
            turret.updateTracking(camera.hasTarget(), camera.getBearingDegrees());

            telemetry.addLine("Procurando AprilTag...");
            telemetry.addData("Has target", camera.hasTarget());
            telemetry.update();

            if (camera.hasTarget()) {
                return true;
            }
        }

        return false;
    }

    private void aimAtAprilTag(long timeoutMs) {
        long startTime = System.currentTimeMillis();

        while (opModeIsActive()
                && System.currentTimeMillis() - startTime < timeoutMs) {

            camera.update();
            turret.updateTracking(camera.hasTarget(), camera.getBearingDegrees());

            boolean aimed = camera.hasTarget()
                    && Math.abs(camera.getBearingDegrees()) <= RConstants.AIM_TOLERANCE_DEGREES;

            telemetry.addLine("Mirando na AprilTag...");
            telemetry.addData("Has target", camera.hasTarget());
            telemetry.addData("Bearing", camera.getBearingDegrees());
            telemetry.addData("Aimed", aimed);
            telemetry.update();

            if (aimed) {
                return;
            }
        }
    }

    private void waitForShooterReady(long timeoutMs) {
        long startTime = System.currentTimeMillis();

        while (opModeIsActive()
                && System.currentTimeMillis() - startTime < timeoutMs) {

            feeder.update();

            telemetry.addLine("Aguardando shooter estabilizar...");
            telemetry.addData("Target RPM", shooter.getTargetRPM());
            telemetry.addData("Current RPM", shooter.getCurrentRPM());
            telemetry.addData("Ready", shooter.isAtTargetRPM());
            telemetry.update();

            if (shooter.isAtTargetRPM()) {
                return;
            }
        }
    }

    private void shootAmount(int amount, long delayBetweenShotsMs) {
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
}