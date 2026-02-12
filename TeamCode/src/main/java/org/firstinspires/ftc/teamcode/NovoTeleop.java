package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name="teleoperado", group ="Linear OpMode")
public class NovoTeleop extends LinearOpMode {

    private DcMotor rodaEsquerda, rodaDireita;
    private DcMotorEx esteira;
    private DcMotor intake;
    private CRServo servoInt;
    ElapsedTime tempoDecorrido = new ElapsedTime();

    @Override
    public void runOpMode() throws InterruptedException {

        rodaDireita = hardwareMap.get(DcMotor.class, "direita");
        rodaEsquerda = hardwareMap.get(DcMotor.class, "esquerda");
        intake =  hardwareMap.get(DcMotor.class, "intake");
        servoInt = hardwareMap.get(CRServo.class, "servoInt");

        rodaDireita.setDirection(DcMotor.Direction.REVERSE);
        rodaEsquerda.setDirection(DcMotor.Direction.FORWARD);

        waitForStart();
        tempoDecorrido.reset();

        while (opModeIsActive()) {

            double direitaPower;
            double esquedaPower;

            double andar = - gamepad1.left_stick_y;
            double girar = gamepad1.right_stick_x;
            direitaPower = Range.clip(andar - girar, -1.0, 1.0);
            esquedaPower = Range.clip(andar + girar, -1.0, 1.0);
            telemetry.addData("Modo", "DIREÇÃO MANUAL");




        }









    }
}







