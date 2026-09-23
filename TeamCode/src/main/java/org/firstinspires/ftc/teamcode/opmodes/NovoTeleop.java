package org.firstinspires.ftc.teamcode.opmodes;

import static android.os.SystemClock.sleep;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.robot.RConstants;
import org.firstinspires.ftc.teamcode.subsystems.drivetrain.DrivetrainConfig;
import org.firstinspires.ftc.teamcode.subsystems.drivetrain.Drivetrain;
import org.firstinspires.ftc.teamcode.localization.GobildaOdometry;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.shooter.Shooter;
import org.firstinspires.ftc.teamcode.util.CalcDistAlvo;
import org.firstinspires.ftc.teamcode.util.MathU;
import org.firstinspires.ftc.teamcode.util.ShooterLista;
import org.firstinspires.ftc.teamcode.vision.AprilTagCamera;

@TeleOp(name = "DECODE TeleOp Completo", group = "Competition")
public class NovoTeleop extends OpMode {

    private Drivetrain drive;
    private AprilTagCamera camera;
    private Shooter shooter;
    private Intake feeder;
    private GobildaOdometry odo;

    private CalcDistAlvo distanceCalculator;
    private ShooterLista shooterTable;

    private boolean lastManualFeedButton = false;
    private boolean shotFiredThisHold = false;

    private double lastTargetRPM = RConstants.DEFAULT_SHOOTER_RPM;
    private double lastShooterDistanceCm = 0.0;

    private boolean auto = false;

    private boolean ligado = false;
    private boolean ultimoB = false;

    @Override
    public void init() {

        // Inicializa todos os sistemas usando os nomes configurados no hardwareMap
        // Se algum nome de motor, servo ou câmera estiverem diferentes no Driver Hub, o erro provavelmente vai aparecer por aqui

        drive = new Drivetrain();
        drive.init(hardwareMap);
        odo.init(hardwareMap);

        if(RConstants.USE_CAMERA){
            camera = new AprilTagCamera();
            camera.init(hardwareMap);
        }

        if(RConstants.USE_SHOOTER){
            shooter = new Shooter();
            shooter.init(hardwareMap);

            distanceCalculator = new CalcDistAlvo();
            shooterTable = new ShooterLista();
        }

        if(RConstants.USE_FEEDER){
            feeder = new Intake();
            feeder.init(hardwareMap);
        }



        telemetry.addLine("DECODE TeleOp iniciado.");
        telemetry.addLine("Shooter fixo - segure o right bumper (gamepad2) perto do gol:");
        telemetry.addLine("o robo gira sozinho ate alinhar com o QR code, calcula a distancia");
        telemetry.addLine("e atira na velocidade certa assim que estiver pronto.");

        telemetry.addData("Camera", RConstants.USE_CAMERA ? "Ativa" : "Desativo");
        telemetry.addData("Shooter", RConstants.USE_SHOOTER ? "Ativa" : "Desativo");
        telemetry.addData("Feeder", RConstants.USE_FEEDER ? "Ativa" : "Desativo");

        telemetry.update();
    }

    @Override
    public void loop() {
        // Atualiza a câmera antes do drive, pra mira automática usar o bearing mais recente.

        updateDrive();


        if(RConstants.USE_CAMERA){
            updateCamera();
        }
        if(RConstants.USE_SHOOTER){
            updateShooter();
        }
        if(RConstants.USE_FEEDER){
            updateFeeder();
        }

        sendTelemetry();
    }

    private void driveForwardCm(double cm, double cm2, double power, long timeoutMs) {
        drive.encoderTurnCm(cm, cm2, power);

        long startTime = System.currentTimeMillis();

        while (drive.isBusy() && System.currentTimeMillis() - startTime < timeoutMs) {

            sleep(20);
        }

        drive.stop();
    }

    private void updateDrive() {
        // O eixo Y do controle vem invertido no FTC.
        // Por isso usamos o sinal negativo para frente ser positivo.
        //double forward = -gamepad1.left_stick_y;

        // O eixo X do analógico direito controla a rotação do robô.
        //double turn = gamepad1.right_stick_x;

        double axial;
        double lateral;
        double yaw;


        // Controle pelo D-pad
        if (gamepad1.dpad_up) {
            axial = 1;
            lateral = 0;
            yaw = 0;
        }
        else if (gamepad1.dpad_down) {
            axial = -1;
            lateral = 0;
            yaw = 0;
        }
        else if (gamepad1.dpad_left) {
            axial = 0;
            lateral = -1;
            yaw = 0;
        }
        else if (gamepad1.dpad_right) {
            axial = 0;
            lateral = 1;
            yaw = 0;
        }
        else {
            // Controle normal pelo joystick
            axial = MathU.aplicarZonaNeutra(-gamepad1.left_stick_y, 0.55);
            lateral = MathU.aplicarZonaNeutra(gamepad1.left_stick_x, 0.55) * 1.1;
            yaw = MathU.aplicarZonaNeutra(gamepad1.right_stick_x, 0.55);
        }

//        if (gamepad1.b && !ultimoB) {
//            auto = !auto; // liga/desliga
//        }

        ligado = !ligado;

//        ultimoB = gamepad1.b;

//        if (auto) {
//            driveForwardCm(10, 10, 0.45, 1000);
//            driveForwardCm(-10, -10, 0.45,1000);
//        }

        if (gamepad1.b && ligado){
            driveForwardCm(10, 10, 0.45, 1000);
            driveForwardCm(-10, -10, 0.45,1000);
        } else {
            drive.stop();
        }

        double speedMultiplier = DrivetrainConfig.DRIVE_POWER_TURBO;

        if (gamepad1.left_bumper) {
            speedMultiplier = DrivetrainConfig.DRIVE_POWER_NORMAL;
        }

//        if (gamepad1.right_bumper) {
//            speedMultiplier = RConstants.DRIVE_POWER_TURBO;
//        }

        // Sem torreta: enquanto o botão de shooter automático (gamepad2) estiver
        // segurado e a câmera estiver vendo o QR code/AprilTag, o próprio chassi
        // gira sozinho até apontar pro gol. O piloto ainda controla axial/lateral
        // (pode se aproximar ou ajustar posição), só o giro (yaw) é sobrescrito.
        boolean autoAimActive = RConstants.USE_CAMERA
                && RConstants.USE_SHOOTER
                && gamepad2.right_bumper
                && camera.hasTarget();

        if (autoAimActive) {
            double targetHeadingDegrees = drive.getHeadingDegrees() + camera.getBearingDegrees();
            yaw = drive.getTurnPowerToHeading(targetHeadingDegrees);
        }

        drive.driveMecanum(axial, lateral, yaw, speedMultiplier);
    }


    private void updateCamera() {
        camera.update();
    }

    private void updateShooter() {
        boolean autoShooterButton = gamepad2.x;
        boolean manualShooterButton = gamepad2.right_bumper;

        //Botão de parada rápida do shooter.
        if (gamepad2.a) {
            shooter.stop();
            return;
        }

        if (autoShooterButton) {
            if (RConstants.USE_CAMERA && camera.hasTarget()) {
                // Pega a distância medida pela AprilTag em polegadas.
                double aprilTagRangeInches = camera.getRangeInches();

                // Converte/ajusta a distância da câmera para a distância real usada pelo shooter.
                lastShooterDistanceCm = distanceCalculator.getShooterDistanceCm(aprilTagRangeInches);

                // Usa a distância calculada para escolher o RPM ideal.
                lastTargetRPM = shooterTable.getRPMForDistance(lastShooterDistanceCm);

                // Manda o shooter tentar atingir o RPM calculado.
                shooter.setRPM(lastTargetRPM);

                // Sem torreta: enquanto o botão está segurado, o chassi gira sozinho
                // (ver updateDrive/autoAimActive) até apontar pro gol usando o bearing
                // da AprilTag. "Aimed" confirma que esse giro já convergiu.
                boolean aimed = Math.abs(camera.getBearingDegrees()) <= RConstants.AIM_TOLERANCE_DEGREES;

                // Verifica se o shooter já chegou perto do RPM desejado.
                boolean shooterReady = shooter.isAtTargetRPM();

                // Dispara assim que o chassi terminar de girar e o shooter estiver no RPM certo -
                // só uma vez por "segurada" do botão (o giro demora um pouco, então não dá
                // mais pra disparar só no instante exato do aperto do botão).
                if (RConstants.USE_FEEDER
                        && !shotFiredThisHold
                        && aimed
                        && shooterReady)
                {
                    feeder.pushOne();
                    shotFiredThisHold = true;
                }

            } else {
                // Se o modo automático estiver ativo, mas não houver AprilTag,
                // usa o RPM padrão como alternativa.
                lastTargetRPM = RConstants.DEFAULT_SHOOTER_RPM;
                shooter.setRPM(lastTargetRPM);
            }

        } else if (manualShooterButton) {
            // Modo manual: gira o shooter no RPM padrão, sem depender da câmera.
            lastTargetRPM = RConstants.DEFAULT_SHOOTER_RPM;
            shooter.setRPM(lastTargetRPM);
            shotFiredThisHold = false;

        } else {
            // Se nenhum botão de shooter estiver pressionado, o shooter para.
            // Para competição, talvez seja melhor trocar isso por um sistema de liga/desliga.
            shooter.stop();
            shotFiredThisHold = false;
        }
    }

    private void updateFeeder() {
        feeder.update();

        // Enquanto A estiver pressionado, o intake continua puxando.
        if (gamepad2.left_bumper) {
            feeder.startContinuous();
        } else if (feeder.isContinuous()) {
            // Soltou A: volta para a posição/potência de repouso.
            feeder.stopContinuous();
        }
    }

    private void sendTelemetry() {
        telemetry.addLine("----- DRIVE -----");
        telemetry.addData("Heading", drive.getHeadingDegrees());


        telemetry.addLine("----- SISTEMAS -----");
        telemetry.addData("Camera", RConstants.USE_CAMERA ? "Ativa" : "Desativo");
        telemetry.addData("Shooter", RConstants.USE_SHOOTER ? "Ativa" : "Desativo");
        telemetry.addData("Feeder", RConstants.USE_FEEDER ? "Ativa" : "Desativo");

        if(RConstants.USE_CAMERA) {
            telemetry.addLine("----- CAMERA (mira + distância) -----");
            telemetry.addData("Has AprilTag", camera.hasTarget());

            // Só mostra os dados detalhados da AprilTag se alguma tag estiver visível.
            // Sem torreta: enquanto o botão de shooter automático estiver segurado,
            // o próprio chassi gira sozinho usando esse Bearing.
            if (camera.hasTarget()) {
                telemetry.addData("Tag ID", camera.getTargetId());
                telemetry.addData("Range inches", camera.getRangeInches());
                telemetry.addData("Bearing", camera.getBearingDegrees());
                telemetry.addData("Yaw", camera.getYawDegrees());
                telemetry.addData("Mira automática ativa", gamepad2.right_bumper);
            }
        }

        if(RConstants.USE_SHOOTER) {
            telemetry.addLine("----- SHOOTER -----");
            telemetry.addData("Distance cm", lastShooterDistanceCm);
            telemetry.addData("Target RPM", lastTargetRPM);
            telemetry.addData("Current RPM", shooter.getCurrentRPM());
            telemetry.addData("Shooter ready", shooter.isAtTargetRPM());
        }

        telemetry.update();
    }


    public void stop() {
        // Garante que os principais sistemas parem quando o OpMode for encerrado.
        if(drive != null){
            drive.stop();
        }

        if(RConstants.USE_SHOOTER && shooter != null){
            shooter.stop();
        }

        if(RConstants.USE_CAMERA && camera != null){
            camera.close();
        }

        if(RConstants.USE_FEEDER && feeder != null){
            feeder.stop();
        }
    }
}


//