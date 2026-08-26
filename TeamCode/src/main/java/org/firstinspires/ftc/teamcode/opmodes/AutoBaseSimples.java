package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.core.RConstants;
import org.firstinspires.ftc.teamcode.drive.DriveSistema2;
import org.firstinspires.ftc.teamcode.mechanisms.IntakeSistema;
import org.firstinspires.ftc.teamcode.mechanisms.ShooterSistema;
import org.firstinspires.ftc.teamcode.util.MathU;
import org.firstinspires.ftc.teamcode.util.ShooterLista;

/**
 * Ações simples que podem ser usadas em qualquer rota autônoma.
 *
 * Esta classe não decide a trajetória. Cada OpMode escreve a sequência de
 * ações na ordem em que deve acontecer, usando chamadas como andarCm(),
 * virarGraus() e atirar().
 */
public abstract class AutoBaseSimples extends LinearOpMode {

    protected DriveSistema2 drive;
    protected ShooterSistema shooter;
    protected IntakeSistema intake;
    protected ShooterLista shooterTable;

    private boolean autoInterrompido = false;

    protected void iniciarSistemas() {
        drive = new DriveSistema2();
        drive.init(hardwareMap);

        shooterTable = new ShooterLista();

        if (RConstants.USE_SHOOTER) {
            shooter = new ShooterSistema();
            shooter.init(hardwareMap);
        }

        if (RConstants.USE_FEEDER) {
            intake = new IntakeSistema();
            intake.init(hardwareMap);
        }
    }

    /** Valor positivo anda para frente; negativo anda de ré. */
    protected void andarCm(double cm, double potencia) {
        if (!podeExecutar()) {
            return;
        }

        drive.encoderDriveCm(cm, potencia);
        long inicio = System.currentTimeMillis();

        while (podeExecutar()
                && drive.isBusy()
                && System.currentTimeMillis() - inicio
                < RConstants.AUTO_DRIVE_TIMEOUT_MS) {

            telemetry.addLine("Andando por encoder");
            telemetry.addData("Pedido (cm)", cm);
            telemetry.addData("Medido (cm)", drive.getAverageEncoderDistanceCm());
            telemetry.update();
            sleep(20);
        }

        boolean terminou = !drive.isBusy();
        drive.stop();

        if (podeExecutar() && !terminou) {
            interromperAuto("Timeout ao andar " + cm + " cm.");
        }
    }

    /** Valor positivo desloca para a direita; negativo, para a esquerda. */
    protected void strafeCm(double cm, double potencia) {
        if (!podeExecutar()) {
            return;
        }

        drive.encoderStrafeCm(cm, potencia);
        long inicio = System.currentTimeMillis();

        while (podeExecutar()
                && drive.isBusy()
                && System.currentTimeMillis() - inicio
                < RConstants.AUTO_DRIVE_TIMEOUT_MS) {

            telemetry.addLine("Deslocamento lateral por encoder");
            telemetry.addData("Pedido (cm)", cm);
            telemetry.addData("Medido (cm)", drive.getAverageStrafeDistanceCm());
            telemetry.update();
            sleep(20);
        }

        boolean terminou = !drive.isBusy();
        drive.stop();

        if (podeExecutar() && !terminou) {
            interromperAuto("Timeout no strafe de " + cm + " cm.");
        }
    }

    /** Valor positivo e negativo giram para lados opostos. */
    protected void virarGraus(double graus) {
        if (!podeExecutar()) {
            return;
        }

        drive.setRunMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        drive.resetTurnController();

        double alvo = MathU.normalizarAngulo(
                drive.getHeadingDegrees() + graus
        );

        long inicio = System.currentTimeMillis();

        while (podeExecutar()
                && System.currentTimeMillis() - inicio
                < RConstants.AUTO_TURN_TIMEOUT_MS) {

            double atual = drive.getHeadingDegrees();
            double erro = MathU.normalizarAngulo(alvo - atual);

            telemetry.addLine("Girando pela IMU");
            telemetry.addData("Giro pedido", graus);
            telemetry.addData("Heading atual", atual);
            telemetry.addData("Heading alvo", alvo);
            telemetry.addData("Erro", erro);
            telemetry.update();

            if (Math.abs(erro) <= RConstants.TURN_TOLERANCE_DEGREES) {
                drive.stop();
                return;
            }

            drive.turnToHeading(alvo);
            sleep(20);
        }

        drive.stop();

        if (podeExecutar()) {
            interromperAuto("Timeout no giro de " + graus + " graus.");
        }
    }

    protected void ligarIntake() {
        if (!podeExecutar()) {
            return;
        }

        if (intake == null) {
            interromperAuto("Intake está desativado.");
            return;
        }

        intake.startContinuous();
    }

    protected void desligarIntake() {
        if (intake != null) {
            intake.stopContinuous();
        }
    }

    /**
     * Calcula o RPM pela distância, espera os dois motores estabilizarem e
     * alimenta a quantidade pedida de bolas.
     */
    protected void atirar(double distanciaDoGolCm, int quantidade) {
        if (!podeExecutar()) {
            return;
        }

        if (shooter == null || intake == null) {
            interromperAuto("Shooter ou intake está desativado.");
            return;
        }

        double rpmAlvo = shooterTable.getRPMForDistance(distanciaDoGolCm);
        shooter.setRPM(rpmAlvo);

        if (!shooterTable.isDistanceInsideTable(distanciaDoGolCm)) {
            telemetry.addLine("ATENÇÃO: distância fora da tabela de RPM.");
            telemetry.addData("Distância pedida", distanciaDoGolCm);
            telemetry.addData(
                    "Faixa calibrada",
                    "%.0f a %.0f cm",
                    shooterTable.getMinimumCalibratedDistanceCm(),
                    shooterTable.getMaximumCalibratedDistanceCm()
            );
            telemetry.addLine("Será usado o RPM do limite mais próximo.");
            telemetry.update();
        }

        boolean pronto = esperarShooter(rpmAlvo);

        if (!pronto && !RConstants.AUTO_SHOOT_AFTER_RPM_TIMEOUT) {
            interromperAuto("Shooter não estabilizou no RPM alvo.");
            return;
        }

        for (int i = 0; i < quantidade && podeExecutar(); i++) {
            intake.pushOne();

            while (podeExecutar() && intake.isBusy()) {
                intake.update();
                telemetry.addData("Disparo", i + 1);
                telemetry.addData("Total", quantidade);
                telemetry.addData("RPM alvo", rpmAlvo);
                telemetry.addData("RPM esquerdo", shooter.getLeftRPM());
                telemetry.addData("RPM direito", shooter.getRightRPM());
                telemetry.update();
                sleep(20);
            }

            if (i < quantidade - 1) {
                esperar(RConstants.AUTO_DELAY_BETWEEN_SHOTS_MS);
            }
        }

        intake.rest();
        shooter.stop();
    }

    protected void esperar(long milissegundos) {
        long inicio = System.currentTimeMillis();

        while (podeExecutar()
                && System.currentTimeMillis() - inicio < milissegundos) {
            telemetry.addData("Aguardando (ms)", milissegundos);
            telemetry.update();
            sleep(20);
        }
    }

    private boolean esperarShooter(double rpmAlvo) {
        long inicio = System.currentTimeMillis();

        while (podeExecutar()
                && System.currentTimeMillis() - inicio
                < RConstants.AUTO_SHOOTER_READY_TIMEOUT_MS) {

            long tempoLigado = System.currentTimeMillis() - inicio;
            boolean tempoMinimo =
                    tempoLigado >= RConstants.SHOOTER_SPINUP_DELAY_MS;
            boolean rpmPronto = shooter.isAtTargetRPM();

            telemetry.addLine("Acelerando os dois motores do shooter");
            telemetry.addData("Tempo ligado (ms)", tempoLigado);
            telemetry.addData("RPM alvo", rpmAlvo);
            telemetry.addData("RPM esquerdo", shooter.getLeftRPM());
            telemetry.addData("RPM direito", shooter.getRightRPM());
            telemetry.addData("Pronto", tempoMinimo && rpmPronto);
            telemetry.update();

            if (tempoMinimo && rpmPronto) {
                return true;
            }

            sleep(20);
        }

        return false;
    }

    protected boolean podeExecutar() {
        return opModeIsActive() && !autoInterrompido;
    }

    protected void interromperAuto(String motivo) {
        autoInterrompido = true;
        pararTudo();
        telemetry.addLine("AUTÔNOMO INTERROMPIDO");
        telemetry.addLine(motivo);
        telemetry.update();
    }

    protected void pararTudo() {
        if (drive != null) {
            drive.stop();
        }

        if (intake != null) {
            intake.stop();
        }

        if (shooter != null) {
            shooter.stop();
        }
    }
}
