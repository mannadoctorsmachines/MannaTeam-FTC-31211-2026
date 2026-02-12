package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;

@Autonomous(name=  "Autônomo", group = "Robot")
public class AutonomoCurto extends LinearOpMode {

    // variaveis do robo
    private DcMotor rodaEsquerda, rodaDireita;
    private DcMotorEx esteira;
    private FuncoesRobo funcoes;
    ElapsedTime runtime = new ElapsedTime();

    // variaveis para o encoder
    static final double DRIVE_SPEED = 0.5;
    static final double TEMPO_TIRO_MS = 4000;  // tempo de disparo (ms)
    //static final double TEMPO_INTAKE_MS = 6000; // tempo do intake (ms)
    //static final double VELOCIDADE_INTAKE_INV = -1.0; // potência do intake


    @Override
    public void runOpMode() throws InterruptedException {

        rodaEsquerda = hardwareMap.get(DcMotor.class, "esquerda");
        rodaDireita = hardwareMap.get(DcMotor.class, "direita");
        esteira = hardwareMap.get(DcMotorEx.class, "esteira");

        rodaEsquerda.setDirection(DcMotor.Direction.REVERSE); //define que o motor gire ao contrario
        rodaDireita.setDirection(DcMotor.Direction.FORWARD); //define que o motor gire no modo padrão

        esteira.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER); //funciona sem o enconder
        esteira.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE); //quando o motor atingir a potencia 0 o motor para

        rodaDireita.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER); //atribui uma função pra resetar e parar os encoders
        rodaEsquerda.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER); //atribui uma função pra resetar e parar os encoders
        rodaDireita.setMode(DcMotor.RunMode.RUN_USING_ENCODER); //atribui uma função para utilizar os encoders
        rodaEsquerda.setMode(DcMotor.RunMode.RUN_USING_ENCODER); //atribui uma função para utilizar os encoders

        funcoes = new FuncoesRobo(this, rodaDireita, rodaEsquerda, esteira); //chama a classe "FuncoesRobo"

        telemetry.addLine("Pronto, pressione START!");
        telemetry.update();

        waitForStart();
        if (!opModeIsActive()) return;

        telemetry.addLine("Andando para trás...");
        telemetry.update();

        funcoes.encoderDrive(DRIVE_SPEED, 1, 1, 3.0);

        //Controlando RPM da esteira
        telemetry.addLine("iniciando RPM...");
        telemetry.update();

        while (opModeIsActive() && runtime.milliseconds() < TEMPO_TIRO_MS) {

            double rpmAtual = funcoes.getRpm();

            double power = (rpmAtual - 100) * 0.07;

            //double error = 3500 - rpmAtual;
            //double power = error * 0.0003;
            //power = Math.max(0, Math.min(power, 1));

            esteira.setPower(power);

            telemetry.addData("RPM Atual", rpmAtual);
            telemetry.addData("Power", power);
            telemetry.update();
        }

    esteira.setPower(0);

        telemetry.addLine("Tiro concluído");
        telemetry.update();
        sleep(1000);

    }

}
