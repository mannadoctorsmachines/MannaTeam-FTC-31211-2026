package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.robot.RConstants;
import org.firstinspires.ftc.teamcode.subsystems.drivetrain.Drivetrain;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.shooter.Shooter;
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

    protected Drivetrain drive;
    protected Shooter shooter;
    protected Intake intake;
    protected ShooterLista shooterTable;

    private boolean autoInterrompido = false;

    protected void iniciarSistemas() {
        drive = new Drivetrain();
        drive.init(hardwareMap);

        shooterTable = new ShooterLista();

        if (RConstants.USE_SHOOTER) {
            shooter = new Shooter();
            shooter.init(hardwareMap);
        }

        if (RConstants.USE_FEEDER) {
            intake = new Intake();
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

    /** Valor positivo e negativo giram a partir da direção atual. */
    protected void virarGraus(double graus) {
        if (!podeExecutar()) {
            return;
        }

        double alvo = MathU.normalizarAngulo(
                drive.getHeadingDegrees() + graus
        );

        executarGiroAte(alvo, "Giro relativo", graus);
    }

    /**
     * Gira para um heading absoluto da IMU. O heading 0 é a direção em que o
     * robô estava no INIT. Isso evita acumular erro entre vários trechos.
     */
    protected void virarParaGraus(double headingAlvo) {
        if (!podeExecutar()) {
            return;
        }

        double alvo = MathU.normalizarAngulo(headingAlvo);
        executarGiroAte(alvo, "Heading absoluto", headingAlvo);
    }

    private void executarGiroAte(
            double alvo,
            String tipoDoPedido,
            double valorPedido
    ) {
        drive.setRunMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        drive.resetTurnController();

        long inicio = System.currentTimeMillis();

        while (podeExecutar()
                && System.currentTimeMillis() - inicio
                < RConstants.AUTO_TURN_TIMEOUT_MS) {

            double atual = drive.getHeadingDegrees();
            double erro = MathU.normalizarAngulo(alvo - atual);

            telemetry.addLine("Girando pela IMU");
            telemetry.addData(tipoDoPedido, valorPedido);
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
            interromperAuto("Timeout ao girar para " + alvo + " graus.");
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
    protected boolean atirar(double distanciaDoGolCm, int quantidade) {
        if (!podeExecutar()) {
            return false;
        }

        if (distanciaDoGolCm <= 0.0 || quantidade <= 0) {
            interromperAuto("Distância ou quantidade de disparos inválida.");
            return false;
        }

        if (shooter == null || intake == null) {
            interromperAuto(
                    "Shooter ou intake não foi inicializado. "
                            + "Confira USE_SHOOTER e USE_FEEDER."
            );
            return false;
        }

        if (!shooter.hasValidEncoderConversion()) {
            interromperAuto("Conversão de encoder do shooter inválida.");
            return false;
        }

        if (!shooter.hasNominalMotorRPMConfigured()) {
            interromperAuto(
                    "RPM nominal do motor do shooter ainda não configurado."
            );
            return false;
        }

        // Garante que o modo contínuo usado na coleta não impeça pushOne().
        // Sem esta parada, o segundo lançamento do ciclo poderia ficar preso.
        intake.stop();

        boolean distanciaCalibrada =
                shooterTable.isDistanceInsideTable(distanciaDoGolCm);

        if (!distanciaCalibrada
                && !RConstants.AUTO_ALLOW_DISTANCE_OUTSIDE_RPM_TABLE) {
            interromperAuto(
                    "Distância " + distanciaDoGolCm
                            + " cm ainda não existe na tabela de RPM."
            );
            return false;
        }

        double rpmAlvo = shooterTable.getRPMForDistance(distanciaDoGolCm);
        shooter.setRPM(rpmAlvo);
        double rpmAplicado = shooter.getTargetRPM();

        if (!distanciaCalibrada) {
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

        boolean pronto = esperarShooter(rpmAplicado);

        if (!pronto && !RConstants.AUTO_SHOOT_AFTER_RPM_TIMEOUT) {
            interromperAuto("Shooter não estabilizou no RPM alvo.");
            return false;
        }

        for (int i = 0; i < quantidade && podeExecutar(); i++) {
            if (i > 0) {
                esperar(RConstants.AUTO_DELAY_BETWEEN_SHOTS_MS);

                boolean recuperou = esperarRecuperacaoShooter(rpmAplicado);

                if (!recuperou && !RConstants.AUTO_SHOOT_AFTER_RPM_TIMEOUT) {
                    interromperAuto(
                            "Shooter não recuperou o RPM antes da bolinha "
                                    + (i + 1) + "."
                    );
                    return false;
                }
            }

            boolean alimentacaoIniciada = intake.pushOne();

            if (!alimentacaoIniciada) {
                interromperAuto(
                        "Intake ocupado antes da bolinha " + (i + 1) + "."
                );
                return false;
            }

            long inicioAlimentacao = System.currentTimeMillis();

            while (podeExecutar()
                    && intake.isBusy()
                    && System.currentTimeMillis() - inicioAlimentacao
                    < RConstants.AUTO_FEED_TIMEOUT_MS) {
                intake.update();
                telemetry.addData("Disparo", i + 1);
                telemetry.addData("Total", quantidade);
                telemetry.addData("RPM pedido do rolo", rpmAlvo);
                telemetry.addData("RPM aplicado do rolo", rpmAplicado);
                telemetry.addData("RPM esquerdo", shooter.getLeftRPM());
                telemetry.addData("RPM direito", shooter.getRightRPM());
                telemetry.update();
                sleep(20);
            }

            boolean alimentacaoTerminou = !intake.isBusy();
            intake.rest();

            if (podeExecutar() && !alimentacaoTerminou) {
                interromperAuto(
                        "Timeout ao alimentar a bolinha " + (i + 1) + "."
                );
                return false;
            }
        }

        intake.rest();
        shooter.stop();
        return podeExecutar();
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

    /**
     * Depois de cada bolinha, espera os rolos voltarem ao RPM alvo antes de
     * liberar a próxima. Aqui não existe outro atraso fixo de dois segundos.
     */
    private boolean esperarRecuperacaoShooter(double rpmAlvo) {
        long inicio = System.currentTimeMillis();

        while (podeExecutar()
                && System.currentTimeMillis() - inicio
                < RConstants.AUTO_SHOOTER_RECOVERY_TIMEOUT_MS) {

            telemetry.addLine("Recuperando RPM após o disparo");
            telemetry.addData("RPM alvo do rolo", rpmAlvo);
            telemetry.addData("RPM esquerdo", shooter.getLeftRPM());
            telemetry.addData("RPM direito", shooter.getRightRPM());
            telemetry.addData("Pronto", shooter.isAtTargetRPM());
            telemetry.update();

            if (shooter.isAtTargetRPM()) {
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
