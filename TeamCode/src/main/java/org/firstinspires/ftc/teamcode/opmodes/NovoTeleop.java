package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.core.RConstants;
import org.firstinspires.ftc.teamcode.drive.DriveSistema2;
import org.firstinspires.ftc.teamcode.mechanisms.IntakeSistema;
import org.firstinspires.ftc.teamcode.mechanisms.ShooterSistema;
import org.firstinspires.ftc.teamcode.util.CalcDistAlvo;
import org.firstinspires.ftc.teamcode.util.MathU;
import org.firstinspires.ftc.teamcode.util.ShooterLista;
import org.firstinspires.ftc.teamcode.vision.AprilTagCamera;

@TeleOp(name = "TeleOp Completo", group = "Competition")
public class NovoTeleop extends OpMode {

    private DriveSistema2 drive;
    private AprilTagCamera camera;
    private ShooterSistema shooter;
    private IntakeSistema feeder;

    private CalcDistAlvo distanceCalculator;
    private ShooterLista shooterTable;

    private boolean lastManualFeedButton = false;
    private boolean shotFiredThisHold = false;

    private double lastTargetRPM = RConstants.DEFAULT_SHOOTER_RPM;
    private double lastShooterDistanceCm = 0.0;

    @Override
    public void init() {

        // Inicializa todos os sistemas usando os nomes configurados no hardwareMap
        // Se algum nome de motor, servo ou câmera estiverem diferentes no Driver Hub, o erro provavelmente vai aparecer por aqui

        drive = new DriveSistema2();
        drive.init(hardwareMap);

        if(RConstants.USE_CAMERA){
            camera = new AprilTagCamera();
            camera.init(hardwareMap);
        }

        if(RConstants.USE_SHOOTER){
            shooter = new ShooterSistema();
            shooter.init(hardwareMap);

            distanceCalculator = new CalcDistAlvo();
            shooterTable = new ShooterLista();
        }

        if(RConstants.USE_FEEDER){
            feeder = new IntakeSistema();
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
        if(RConstants.USE_CAMERA){
            updateCamera();
        }

        updateDrive();

        if(RConstants.USE_SHOOTER){
            updateShooter();
        }
        if(RConstants.USE_FEEDER){
            updateFeeder();
        }

        sendTelemetry();
    }

    private void updateDrive() {
        // O eixo Y do controle vem invertido no FTC.
        // Por isso usamos o sinal negativo para frente ser positivo.
        //double forward = -gamepad1.left_stick_y;

        // O eixo X do analógico direito controla a rotação do robô.
        //double turn = gamepad1.right_stick_x;

        double axial = MathU.aplicarZonaNeutra(-gamepad1.left_stick_y, 0.05);
        double lateral = MathU.aplicarZonaNeutra(gamepad1.left_stick_x, 0.05) * 1.1;
        double yaw = MathU.aplicarZonaNeutra(gamepad1.right_stick_x, 0.05);

        double speedMultiplier = RConstants.DRIVE_POWER_NORMAL;

        if (gamepad1.left_bumper) {
            speedMultiplier = RConstants.DRIVE_POWER_SLOW;
        }

        if (gamepad1.right_bumper) {
            speedMultiplier = RConstants.DRIVE_POWER_TURBO;
        }

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
        boolean autoShooterButton = gamepad2.right_bumper;
        boolean manualShooterButton = gamepad2.left_bumper;

        //Botão de parada rápida do shooter.
        if (gamepad2.b) {
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
        // Atualiza o mecanismo do feeder.
        // Provavelmente controla o tempo de movimento do servo/motor.
        feeder.update();

        boolean manualFeedButton = gamepad2.a;

        // Detecta o clique único no botão A.
        boolean manualFeedJustPressed = manualFeedButton && !lastManualFeedButton;

        // Empurra uma bolinha manualmente quando o operador aperta A.
        if (manualFeedJustPressed) {
            feeder.pushOne();
        }

        // Salva o estado atual do botão para evitar múltiplos acionamentos.
        lastManualFeedButton = manualFeedButton;
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