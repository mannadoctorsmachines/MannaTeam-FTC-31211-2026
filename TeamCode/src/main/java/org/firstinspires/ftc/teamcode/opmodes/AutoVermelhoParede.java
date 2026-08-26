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
        virarGraus(-45.0);
        atirar(335.0, 3);

        pararTudo();
    }
}
