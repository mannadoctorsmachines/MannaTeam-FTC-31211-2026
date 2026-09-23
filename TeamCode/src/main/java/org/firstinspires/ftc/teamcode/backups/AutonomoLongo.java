package org.firstinspires.ftc.teamcode.backups;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.robot.RConstants;
import org.firstinspires.ftc.teamcode.subsystems.drivetrain.Drivetrain;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.shooter.Shooter;
import org.firstinspires.ftc.teamcode.util.CalcDistAlvo;
import org.firstinspires.ftc.teamcode.util.MathU;
import org.firstinspires.ftc.teamcode.util.ShooterLista;
import org.firstinspires.ftc.teamcode.vision.AprilTagCamera;

/**
 * Autônomo adaptado para shooter fixo (sem torreta).
 *
 * A mira agora é feita girando o chassi inteiro: a câmera fornece o bearing
 * até a AprilTag, esse bearing vira um heading alvo (heading atual + bearing)
 * e o drive gira o robô até lá usando o PID de giro que já existia
 * (DriveSistema2.turnToHeading), a mesma lógica usada para virar em outras
 * partes do autônomo.
 *
 * A distância até o alvo continua vindo da câmera (range da AprilTag),
 * como já era antes - o robô não tem sensor ultrassônico.
 */
@Autonomous(name = "DECODE Auto Completo", group = "Competition")
public class AutonomoLongo extends LinearOpMode {

    private Drivetrain drive;

    private AprilTagCamera camera;
    private Shooter shooter;
    private Intake feeder;

    private CalcDistAlvo distanceCalculator;
    private ShooterLista shooterTable;

    @Override
    public void runOpMode() {
        initSystems();

        telemetry.addLine("Autônomo DECODE pronto (shooter fixo).");
        telemetry.addData("Camera", RConstants.USE_CAMERA ? "Ativa" : "Desativada");
        telemetry.addData("Shooter", RConstants.USE_SHOOTER ? "Ativo" : "Desativado");
        telemetry.addData("Feeder", RConstants.USE_FEEDER ? "Ativo" : "Desativado");
        telemetry.update();

        waitForStart();

        if (isStopRequested()) {
            stopAll();
            return;
        }


        //Teste de posição:
        driveForwardCm(-45, -45, 0.45, 2000);

        //Teste de giro:
        turnRobotDegrees(90, 1000);



        boolean foundTag = false;

        if (RConstants.USE_CAMERA) {
            foundTag = searchForAprilTag(2500);
        }

        double targetRPM = RConstants.DEFAULT_SHOOTER_RPM;

        if (foundTag) {
            aimByTurningChassis(RConstants.AIM_TURN_TIMEOUT_MS);

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
            feeder = new Intake();
            feeder.init(hardwareMap);
        }
    }

    private void driveForwardCm(double cm, double cm2, double power, long timeoutMs) {
        drive.encoderTurnCm(cm, cm2, power);

        long startTime = System.currentTimeMillis();

        while (opModeIsActive()
                && drive.isBusy()
                && System.currentTimeMillis() - startTime < timeoutMs) {

            telemetry.addLine("Andando com encoder...");
            telemetry.addData("cm", cm);
            telemetry.addData("cm", cm2);
            telemetry.update();

            sleep(20);
        }

        drive.stop();
    }

    private void turnRobotToHeading(double targetHeadingDegrees, long timeoutMs) {
        // Permite controlar os motores diretamente pelo PID.
        drive.setRunMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        long startTime = System.currentTimeMillis();

        while (opModeIsActive()
                && System.currentTimeMillis() - startTime < timeoutMs) {

            double currentHeading = drive.getHeadingDegrees();

            double headingError = MathU.normalizarAngulo(
                    targetHeadingDegrees - currentHeading
            );

            telemetry.addLine("Virando o robô...");
            telemetry.addData("Ângulo atual", currentHeading);
            telemetry.addData("Ângulo desejado", targetHeadingDegrees);
            telemetry.addData("Erro", headingError);
            telemetry.update();

            if (Math.abs(headingError) <= RConstants.TURN_TOLERANCE_DEGREES) {
                break;
            }

            drive.turnToHeading(targetHeadingDegrees);

            sleep(20);
        }

        drive.stop();
    }

    private void turnRobotDegrees(double degrees, long timeoutMs){
        double targetHeading = drive.getHeadingDegrees() + degrees;
        turnRobotToHeading(targetHeading, timeoutMs);
    }

    private boolean searchForAprilTag(long timeoutMs) {
        if (camera == null) {
            return false;
        }

        long startTime = System.currentTimeMillis();

        while (opModeIsActive()
                && System.currentTimeMillis() - startTime < timeoutMs) {

            camera.update();

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

    /**
     * Gira o chassi inteiro (sem torreta) até o bearing da AprilTag
     * ficar dentro da tolerância de mira configurada.
     */
    private void aimByTurningChassis(long timeoutMs) {
        if (camera == null) {
            return;
        }

        // O movimento anterior deixou os motores em RUN_TO_POSITION.
        // Para girar pelo PID, precisamos devolver o controle direto de potência.
        drive.setRunMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        long startTime = System.currentTimeMillis();

        while (opModeIsActive()
                && System.currentTimeMillis() - startTime < timeoutMs) {

            camera.update();

            if (!camera.hasTarget()) {
                drive.stop();
                sleep(20);
                continue;
            }

            double bearingDegrees = camera.getBearingDegrees();

            boolean aimed =
                    Math.abs(bearingDegrees)
                            <= RConstants.AIM_TOLERANCE_DEGREES;

            telemetry.addLine("Mirando com o chassi...");
            telemetry.addData("Bearing", bearingDegrees);
            telemetry.addData("Aimed", aimed);
            telemetry.update();

            if (aimed) {
                drive.stop();
                return;
            }

            double targetHeadingDegrees =
                    drive.getHeadingDegrees() + bearingDegrees;

            drive.turnToHeading(targetHeadingDegrees);

            sleep(20);
        }

        drive.stop();
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

            // Espera o movimento de um disparo terminar.
            while (opModeIsActive() && feeder.isBusy()) {
                feeder.update();

                telemetry.addData("Disparando", i + 1);
                telemetry.addData("Total", amount);
                telemetry.update();

                sleep(20);
            }

            // Intervalo adicional antes do próximo disparo.
            if (i < amount - 1) {
                sleep(delayBetweenShotsMs);
            }
        }

        feeder.rest();
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

        if (camera != null) {
            camera.close();
        }
    }
}