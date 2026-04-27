package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.RConstants;
import org.firstinspires.ftc.teamcode.drive.DriveSistema;
import org.firstinspires.ftc.teamcode.mechanisms.CameraTurret;
import org.firstinspires.ftc.teamcode.mechanisms.IntakeSistema;
import org.firstinspires.ftc.teamcode.mechanisms.ShooterSistema;
import org.firstinspires.ftc.teamcode.util.CalcDistAlvo;
import org.firstinspires.ftc.teamcode.util.ShooterLista;
import org.firstinspires.ftc.teamcode.vision.AprilTagCamera;

@TeleOp(name = "DECODE TeleOp Completo", group = "Competition")
public class NovoTeleop extends OpMode {

    private DriveSistema drive;
    private AprilTagCamera camera;
    private CameraTurret turret;
    private ShooterSistema shooter;
    private IntakeSistema feeder;

    private CalcDistAlvo distanceCalculator;
    private ShooterLista shooterTable;

    private boolean lastShooterButton = false;
    private boolean lastManualFeedButton = false;

    private double lastTargetRPM = RConstants.DEFAULT_SHOOTER_RPM;
    private double lastShooterDistanceCm = 0.0;

    @Override
    public void init() {
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

        telemetry.addLine("DECODE TeleOp iniciado.");
        telemetry.addLine("Right bumper gamepad2 = shooter automático.");
        telemetry.update();
    }

    @Override
    public void loop() {
        updateCameraAndTurret();
        updateDrive();
        updateShooter();
        updateFeeder();
        sendTelemetry();
    }

    private void updateCameraAndTurret() {
        camera.update();

        if (gamepad2.y) {
            turret.enableTracking();
        }

        if (gamepad2.x) {
            turret.disableTracking();
        }

        turret.updateTracking(
                camera.hasTarget(),
                camera.getBearingDegrees()
        );
    }

    private void updateDrive() {
        double forward = -gamepad1.left_stick_y;
        double turn = gamepad1.right_stick_x;

        double speedMultiplier = RConstants.DRIVE_POWER_NORMAL;

/*        if (gamepad1.left_bumper) {
            speedMultiplier = RConstants.DRIVE_POWER_SLOW;
        }

        if (gamepad1.right_bumper) {
            speedMultiplier = RConstants.DRIVE_POWER_TURBO;
        }
*/

        drive.driveArcade(forward, turn, speedMultiplier);
    }



    private void updateShooter() {
        boolean autoShooterButton = gamepad2.right_bumper;
        boolean manualShooterButton = gamepad2.left_bumper;

        if (gamepad2.b) {
            shooter.stop();
            return;
        }

        if (autoShooterButton) {
            if (camera.hasTarget()) {
                double aprilTagRangeInches = camera.getRangeInches();

                lastShooterDistanceCm = distanceCalculator.getShooterDistanceCm(aprilTagRangeInches);
                lastTargetRPM = shooterTable.getRPMForDistance(lastShooterDistanceCm);

                shooter.setRPM(lastTargetRPM);

                boolean buttonJustPressed = autoShooterButton && !lastShooterButton;
                boolean aimed = Math.abs(camera.getBearingDegrees()) <= RConstants.AIM_TOLERANCE_DEGREES;
                boolean shooterReady = shooter.isAtTargetRPM();

                if (buttonJustPressed && aimed && shooterReady) {
                    feeder.pushOne();
                }
            } else {

                lastTargetRPM = RConstants.DEFAULT_SHOOTER_RPM;
                shooter.setRPM(lastTargetRPM);
            }
        } else if (manualShooterButton) {

            lastTargetRPM = RConstants.DEFAULT_SHOOTER_RPM;
            shooter.setRPM(lastTargetRPM);
        } else {

             //Se quiser que o shooter continue girando depois de soltar o botão, remova esta linha.

            shooter.stop();
        }

        lastShooterButton = autoShooterButton;
    }

    private void updateFeeder() {
        feeder.update();

        boolean manualFeedButton = gamepad2.a;
        boolean manualFeedJustPressed = manualFeedButton && !lastManualFeedButton;

        if (manualFeedJustPressed) {
            feeder.pushOne();
        }

        lastManualFeedButton = manualFeedButton;
    }

    private void sendTelemetry() {
        telemetry.addLine("----- DRIVE -----");
        telemetry.addData("Heading", drive.getHeadingDegrees());

        telemetry.addLine("----- CAMERA -----");
        telemetry.addData("Tracking", turret.isTrackingEnabled());
        telemetry.addData("Has AprilTag", camera.hasTarget());

        if (camera.hasTarget()) {
            telemetry.addData("Tag ID", camera.getTargetId());
            telemetry.addData("Range inches", camera.getRangeInches());
            telemetry.addData("Bearing", camera.getBearingDegrees());
            telemetry.addData("Yaw", camera.getYawDegrees());
        }

        telemetry.addLine("----- SHOOTER -----");
        telemetry.addData("Distance cm", lastShooterDistanceCm);
        telemetry.addData("Target RPM", lastTargetRPM);
        telemetry.addData("Current RPM", shooter.getCurrentRPM());
        telemetry.addData("Shooter ready", shooter.isAtTargetRPM());

        telemetry.update();
    }

    @Override
    public void stop() {
        drive.stop();
        shooter.stop();
        turret.stop();
        camera.close();
    }
}