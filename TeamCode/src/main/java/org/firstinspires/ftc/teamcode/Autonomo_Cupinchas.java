package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;

@Autonomous(name="Autonomo longo", group="Robot")
public class Autonomo_Cupinchas extends LinearOpMode {

    private DcMotor leftDrive = null;
    private DcMotor rightDrive = null;
    private DcMotorEx esteira = null;
    private CRServo ServoInt = null;
    private DcMotor intake  = null;
    private ElapsedTime runtime = new ElapsedTime();

    // Robo
    static final double COUNTS_PER_MOTOR_REV = 1440; // Encoder TETRIX (ajuste se usar outro motor)
    static final double DRIVE_GEAR_REDUCTION = 1.0;   // 1:1 direto
    static final double WHEEL_DIAMETER_INCHES = 3.55; // diâmetro das rodas
    static final double COUNTS_PER_INCH = (COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) / (WHEEL_DIAMETER_INCHES * 3.1415);
    static final double DRIVE_SPEED = 0.5;
    static final double VELOCIDADE_INTAKE_INV = -1.0; // potência do intake
    static final double TEMPO_TIRO_MS = 4000;  // tempo de disparo (ms)
    static final double TEMPO_INTAKE_MS = 6000; // tempo do intake (ms)

    double esteiraRPM = 0;

    @Override
    public void runOpMode() {
        // Inicialização
        leftDrive = hardwareMap.get(DcMotor.class, "esquerda");
        rightDrive = hardwareMap.get(DcMotor.class, "direita");
        esteira = hardwareMap.get(DcMotorEx.class, "esteira");
        ServoInt = hardwareMap.get(CRServo.class, "ServoInt");
        intake = hardwareMap.get(DcMotor.class, "intake");

        leftDrive.setDirection(DcMotor.Direction.REVERSE);
        rightDrive.setDirection(DcMotor.Direction.FORWARD);

        leftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        esteira.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        esteira.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        telemetry.addLine("Autônomo pronto. Aguardando START...");
        telemetry.update();

        waitForStart();
        if (!opModeIsActive()) return;


        // andar pra trás antes do tiro
        telemetry.addLine("Recuando para posição de tiro...");
        telemetry.update();

        encoderDrive(DRIVE_SPEED, -9.2, -9.2, 3.0);

        // controle de RPM da esteira
        telemetry.addLine("Iniciando tiro...");
        telemetry.update();

        double ticksPorRevolucao = 28.0; // motor REV padrão
        runtime.reset();

        // mantém o tiro ativo por TEMPO_TIRO_MS milissegundos
        while (opModeIsActive() && runtime.milliseconds() < TEMPO_TIRO_MS) {

            double ticksPorSegundo = esteira.getVelocity(); // velocidade atual em ticks/s
            esteiraRPM = Math.abs((ticksPorSegundo / ticksPorRevolucao) * 60.0); // converte pra RPM

            // Controle de velocidade: tenta manter perto de 4100 RPM
            double power = (esteiraRPM - 3500) * 0.07;
            esteira.setPower(power);

            telemetry.addData("Esteira RPM", esteiraRPM);
            telemetry.addData("Power", power);
            telemetry.update();
        }

        // Agora ativa o intake e o servo para alimentar as peças
      //  ServoInt.setPower(VELOCIDADE_INTAKE_INV);
      //  intake.setPower(VELOCIDADE_INTAKE_INV);
      //  sleep((long) TEMPO_INTAKE_MS);

        //parar tudo
      //  intake.setPower(0);
     //   ServoInt.setPower(0);
      //  esteira.setPower(0);


        telemetry.addLine("Tiro concluído");
        telemetry.update();
        sleep(1000);
    }

    // Função para movimentar com encoder
    public void encoderDrive(double speed, double leftInches, double rightInches, double timeoutS) {
        int newLeftTarget;
        int newRightTarget;

        if (opModeIsActive()) {
            newLeftTarget = leftDrive.getCurrentPosition() + (int)(leftInches * COUNTS_PER_INCH);
            newRightTarget = rightDrive.getCurrentPosition() + (int)(rightInches * COUNTS_PER_INCH);

            leftDrive.setTargetPosition(newLeftTarget);
            rightDrive.setTargetPosition(newRightTarget);

            leftDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            rightDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            runtime.reset();
            leftDrive.setPower(Math.abs(speed));
            rightDrive.setPower(Math.abs(speed));

            while (opModeIsActive() &&
                    runtime.seconds() < timeoutS &&
                    (leftDrive.isBusy() && rightDrive.isBusy())) {
                telemetry.addData("Alvo", "%7d:%7d", newLeftTarget, newRightTarget);
                telemetry.addData("Atual", "%7d:%7d", leftDrive.getCurrentPosition(), rightDrive.getCurrentPosition());
                telemetry.update();
            }

            leftDrive.setPower(0);
            rightDrive.setPower(0);

            leftDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            rightDrive.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

            sleep(250);
        }
    }
}

