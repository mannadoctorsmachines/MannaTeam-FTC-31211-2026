package org.firstinspires.ftc.teamcode;


import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;

public class FuncoesRobo {
    DcMotor rodaEsquerda, rodaDireita;
    DcMotorEx esteira;
    ElapsedTime tempoDecorrido = new ElapsedTime();
    LinearOpMode opMode;
    double ticksPorRevolucao = 28.0;

    static final double COUNTS_PER_MOTOR_REV = 28;  //
    static final double DRIVE_GEAR_REDUCTION = 12.0;   //
    static final double WHEEL_DIAMETER_METERS = 0.15; //
    static final double COUNTS_PER_METER =(COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) /(WHEEL_DIAMETER_METERS * Math.PI);

    //static final double COUNTS_PER_METER = 47560;

    public FuncoesRobo(LinearOpMode opMode, DcMotor right, DcMotor left, DcMotorEx esteira){
        this.opMode = opMode;
        this.rodaDireita = right;
        this.rodaEsquerda = left;
        this.esteira = esteira;
    }

    public double getRpm(){
        double ticksPorSegundo = esteira.getVelocity();  // retorna ticks/s
        return Math.abs((ticksPorSegundo / ticksPorRevolucao) * 60.0);
    }

    public void encoderDrive(double speed, double leftMeters, double rightMeters, double timeoutS) {
        int newLeftTarget;
        int newRightTarget;

            newLeftTarget = rodaEsquerda.getCurrentPosition() + (int)(leftMeters * COUNTS_PER_METER);
            newRightTarget = rodaDireita.getCurrentPosition() + (int)(rightMeters * COUNTS_PER_METER);

            rodaEsquerda.setTargetPosition(newLeftTarget);
            rodaDireita.setTargetPosition(newRightTarget);

            rodaEsquerda.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            rodaDireita.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            tempoDecorrido.reset();
            rodaEsquerda.setPower(Math.abs(speed));
            rodaDireita.setPower(Math.abs(speed));

            while (tempoDecorrido.seconds() < timeoutS && (rodaEsquerda.isBusy() && rodaDireita.isBusy())){
             opMode.telemetry.addData("Alvo", "%7d:%7d", newLeftTarget, newRightTarget);
             opMode.telemetry.addData("Atual", "%7d:%7d", rodaEsquerda.getCurrentPosition(), rodaDireita.getCurrentPosition());
             opMode.telemetry.update();
            }

            rodaEsquerda.setPower(0);
            rodaDireita.setPower(0);

            rodaEsquerda.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            rodaDireita.setMode(DcMotor.RunMode.RUN_USING_ENCODER);




        }
    }


/* OBSERVAÇÔES: Um encoder é um dispositivo eletromecânico ou uma função que converte um movimento físico
(como rotação ou deslocamento linear) em um sinal elétrico, geralmente na forma de pulsos
 */


