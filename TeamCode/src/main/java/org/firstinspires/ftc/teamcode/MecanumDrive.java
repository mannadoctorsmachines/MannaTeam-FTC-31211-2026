package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name="teleoperado", group ="Linear OpMode")
public class MecanumDrive extends LinearOpMode {

    private DcMotor motorFrenteEsquerdo, motorFrenteDireito, motorTrasEsquerdo, motorTrasDireito;
    private DcMotorEx esteira;
    private DcMotor intake;
    private CRServo servoInt;
    ElapsedTime tempoDecorrido = new ElapsedTime();

    @Override
    public void runOpMode() throws InterruptedException {

        motorFrenteDireito = hardwareMap.get(DcMotor.class, "frente direita");
        motorFrenteEsquerdo = hardwareMap.get(DcMotor.class, "frente esquerda");
        motorTrasDireito = hardwareMap.get(DcMotor.class, "tras direita");
        motorTrasEsquerdo = hardwareMap.get(DcMotor.class, "tras esquerda");

        intake = hardwareMap.get(DcMotor.class, "intake");
        servoInt = hardwareMap.get(CRServo.class, "servoInt");

        motorFrenteEsquerdo.setDirection(DcMotor.Direction.REVERSE);
        motorTrasEsquerdo.setDirection(DcMotor.Direction.REVERSE);
        motorFrenteDireito.setDirection(DcMotor.Direction.FORWARD);
        motorTrasDireito.setDirection(DcMotor.Direction.FORWARD);

        motorFrenteDireito.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorTrasDireito.setMode(DcMotor.RunMode.RUN_USING_ENCODER);


        waitForStart();
        tempoDecorrido.reset();


        if (!isStarted()) return;

        while (opModeIsActive()) {

            double y = -gamepad1.left_stick_y;
            double x = gamepad1.left_stick_x * 1.1;
            double rx = gamepad1.right_stick_x;

            double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);
            double frenteEsquerdaPower = (y + x + rx) / denominator;
            double trasEsquerdaPower = (y - x + rx) / denominator;
            double frenteDireitaPower = (y - x - rx) / denominator;
            double trasDireitaPower = (y + x - rx) / denominator;

            motorFrenteDireito.setPower(frenteDireitaPower);
            motorFrenteEsquerdo.setPower(frenteEsquerdaPower);
            motorTrasDireito.setPower(trasDireitaPower);
            motorTrasEsquerdo.setPower(trasEsquerdaPower);

        }
    }
}


