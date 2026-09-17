package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

@Autonomous(name = "DECODE 2 - Vermelho Parede", group = "Competition")
public class AutoVermelhoParede extends AutoBaseSimples {

    @Override
    public void runOpMode() {
        iniciarSistemas();

        telemetry.addLine("VERMELHO: encoste o robô reto na parede traseira.");
        telemetry.addLine("Após o START ele somente gira e dispara.");
        telemetry.update();

        waitForStart();

        if (isStopRequested()) {
            pararTudo();
            return;
        }

        // ROTA VERMELHA. O giro é oposto ao azul.
        andarCm(30.0, 0.45);
        virarGraus(-45.0);
        atirar(100.0, 3);

        pararTudo();
    }
}
