package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

@Autonomous(name = "DECODE 1 - Azul Parede", group = "Competition")
public class AutoAzulParede extends AutoBaseSimples {

    @Override
    public void runOpMode() {
        iniciarSistemas();

        telemetry.addLine("AZUL: encoste o robô reto na parede traseira.");
        telemetry.addLine("Após o START ele somente gira e dispara.");
        telemetry.update();

        waitForStart();

        if (isStopRequested()) {
            pararTudo();
            return;
        }

        // ROTA AZUL. Altere estes números diretamente após medir na arena.
        virarGraus(45.0);
        atirar(335.0, 3);

        pararTudo();
    }
}
