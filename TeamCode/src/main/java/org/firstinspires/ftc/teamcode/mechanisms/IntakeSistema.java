package org.firstinspires.ftc.teamcode.mechanisms;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.core.RConstants;

public class IntakeSistema {

    private DcMotorEx feederMotor;

    private boolean pushing = false;
    private long pushStartTime = 0;

    public void init(HardwareMap hardwareMap) {
        feederMotor = hardwareMap.get(DcMotorEx.class, RConstants.FEEDER);

        feederMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        feederMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        rest();
    }

    public void update() {
        if (!pushing) {
            return;
        }

        long elapsedTime = System.currentTimeMillis() - pushStartTime;

        if (elapsedTime >= RConstants.INTAKE_PUSH_TIME_MS) {
            rest();
            pushing = false;
        }
    }

    public void pushOne() {
        if (pushing) {
            return;
        }

        feederMotor.setPower(RConstants.INTAKE_PUSH_POWER);
        pushStartTime = System.currentTimeMillis();
        pushing = true;
    }

    public void rest() {
        feederMotor.setPower(RConstants.INTAKE_REST_POWER);
    }

    public void stop() {
        pushing = false;
        rest();
    }

    public boolean isBusy() {
        return pushing;
    }
}