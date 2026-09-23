package org.firstinspires.ftc.teamcode.subsystems.intake;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.robot.RConstants;

public class Intake {

    private DcMotorEx feederMotor;

    private boolean pushing = false;
    private boolean continuous = false;
    private boolean reversing = false;
    private long pushStartTime = 0;

    public void init(HardwareMap hardwareMap) {
        feederMotor = hardwareMap.get(
                DcMotorEx.class,
                RConstants.FEEDER
        );

        feederMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        feederMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        rest();
    }

    public void update() {
        // No modo contínuo, não usa o temporizador.
        if (continuous || !pushing) {
            return;
        }

        long elapsedTime =
                System.currentTimeMillis() - pushStartTime;

        if (elapsedTime >= RConstants.INTAKE_PUSH_TIME_MS) {
            rest();
        }
    }

    /**
     * Alimentação temporizada: empurra apenas uma bola.
     */
    public boolean pushOne() {
        if (pushing || continuous) {
            return false;
        }

        reversing = false;
        feederMotor.setPower(RConstants.INTAKE_PUSH_POWER);
        pushStartTime = System.currentTimeMillis();
        pushing = true;
        return true;
    }

    /**
     * Mantém o feeder funcionando continuamente.
     */
    public void startContinuous() {
        continuous = true;
        pushing = true;
        reversing = false;

        feederMotor.setPower(RConstants.INTAKE_PUSH_POWER);
    }

    /**
     * Mantém o intake girando ao contrário para soltar uma bola presa.
     */
    public void startReverseContinuous() {
        continuous = true;
        pushing = true;
        reversing = true;

        feederMotor.setPower(RConstants.INTAKE_REVERSE_POWER);
    }

    /**
     * Para o funcionamento contínuo.
     */
    public void stopContinuous() {
        continuous = false;
        rest();
    }

    public void rest() {
        pushing = false;
        reversing = false;
        feederMotor.setPower(RConstants.INTAKE_REST_POWER);
    }

    public void stop() {
        continuous = false;
        rest();
    }

    public boolean isBusy() {
        return pushing;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public boolean isReversing() {
        return reversing;
    }
}
