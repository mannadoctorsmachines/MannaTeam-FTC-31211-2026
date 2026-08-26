package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

/**
 * Uma única rota para azul ou vermelho.
 *
 * Em cada aliança, o robô deve ser colocado na posição equivalente espelhada:
 * shooter voltado para o próprio gol e alinhado de frente/ré com a fileira de
 * três bolas. Não existe seleção por gamepad.
 */
@Autonomous(name = "DECODE 3 - Ciclo 6 Bolas", group = "Competition")
public class AutoCicloSeisBolas extends AutoBaseSimples {

    @Override
    public void runOpMode() {
        iniciarSistemas();

        telemetry.addLine("CICLO 6 BOLAS - serve para azul ou vermelho.");
        telemetry.addLine("Use a posição espelhada do seu lado da arena.");
        telemetry.addLine("Shooter para o gol e robô alinhado com as 3 bolas.");
        telemetry.update();

        waitForStart();

        if (isStopRequested()) {
            pararTudo();
            return;
        }

        // ROTA: cada linha é uma ação e pode ser alterada separadamente.

        // 1) Sai de ré do gol e lança as 3 bolas que já estão no robô.
        andarCm(-60.0, 0.45);
        atirar(100.0, 3);

        // 2) Vira o intake frontal para as bolas e tenta coletar mais 3.
        virarGraus(180.0);
        ligarIntake();
        andarCm(85.0, 0.35);
        esperar(700);
        desligarIntake();

        // 3) Volta à posição de tiro, aponta para o gol e lança novamente.
        andarCm(-85.0, 0.45);
        virarGraus(180.0);
        atirar(100.0, 3);

        pararTudo();
    }
}
