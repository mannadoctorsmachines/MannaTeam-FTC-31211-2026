package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

/**
 * Rota convertida do arquivo path_robot.pp para movimentos simples por
 * encoder e headings absolutos da IMU. Não depende de Pedro Pathing e não lê
 * nenhum gamepad.
 */
@Autonomous(name = "DECODE 3 - Ciclo 6 Bolas- Vermelho", group = "Competition")
public class AutoCicloSeisBolas extends AutoBaseSimples {

    @Override
    public void runOpMode() {
        iniciarSistemas();

        telemetry.addLine("CICLO 6 BOLAS.");
        telemetry.addLine("Coloque o robô na posição inicial desenhada.");
        telemetry.addLine("No INIT, a IMU considera essa direção como 0 grau.");
        telemetry.update();

        waitForStart();

        if (isStopRequested()) {
            pararTudo();
            return;
        }

        // ROTA CONVERTIDA: todas as distâncias estão em centímetros.
        // virarParaGraus() usa a direção inicial do robô como heading 0.

        // 1) Ponto inicial -> posição de tiro.
        // O robô vira aproximadamente 45 graus e percorre o trecho de ré,
        // mantendo o shooter apontado para o gol.
        andarCm(-100.0, 0.45);
        esperar(500);

        // A posição final deste trecho também será usada no segundo disparo.
        // Ajuste 100 cm para a distância real medida entre o shooter e o gol.
        atirar(100.0, 3);

        // 2) Recua da posição de tiro e começa a busca das bolas próximas.

        // andarCm(-25.0, 0.40);
        virarGraus(45.0);

        esperar(200);

        ligarIntake();
        andarCm(50, 0.45);
        esperar(800);

        andarCm(-50, 0.45);
        virarGraus(-45.9);

        //-25.7, 0.45);;

        atirar(100.0, 3);
        desligarIntake();


        pararTudo();
    }
}
