package org.firstinspires.ftc.teamcode.mechanisms;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.core.RConstants;

public class IntakeSistema {

    private DcMotorEx feederMotor;

    private boolean pushing = false;
    private boolean continuous = false;
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
    public void pushOne() {
        if (pushing || continuous) {
            return;
        }

        feederMotor.setPower(RConstants.INTAKE_PUSH_POWER);
        pushStartTime = System.currentTimeMillis();
        pushing = true;
    }

    /**
     * Mantém o feeder funcionando continuamente.
     */
    public void startContinuous() {
        continuous = true;
        pushing = true;

        feederMotor.setPower(RConstants.INTAKE_PUSH_POWER);
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
}