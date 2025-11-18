package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;

@Autonomous(name=  "Autônomo", group = "Robot")
public class estudos extends LinearOpMode {

    // variaveis do robo
    private DcMotor rodaEsquerda, rodaDireita;
    private FuncoesRobo funcoes;

    // variaveis para o encoder
    static final double DRIVE_SPEED = 0.5;
    static final double VELOCIDADE_INTAKE_INV = -1.0; // potência do intake
    static final double TEMPO_TIRO_MS = 4000;  // tempo de disparo (ms)
    static final double TEMPO_INTAKE_MS = 6000; // tempo do intake (ms)


    @Override
    public void runOpMode() throws InterruptedException {

        rodaEsquerda = hardwareMap.get(DcMotor.class, "esquerda");
        rodaDireita = hardwareMap.get(DcMotor.class, "direita");

        rodaEsquerda.setDirection(DcMotor.Direction.REVERSE);
        rodaDireita.setDirection(DcMotor.Direction.FORWARD);

        rodaDireita.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rodaEsquerda.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rodaDireita.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rodaEsquerda.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        funcoes = new FuncoesRobo(this, rodaDireita, rodaEsquerda);

        telemetry.addLine("Pronto, pressione START!");
        telemetry.update();

        waitForStart();
        if (!opModeIsActive()) return;

        telemetry.addLine("Andando para trás...");
        telemetry.update();

        funcoes.encoderDrive(DRIVE_SPEED, -2, -2, 3.0);

        //Cotrolando RPM da esteira
        telemetry.addLine("iniciando RPM...");
        telemetry.update();



    }
}
 public void
