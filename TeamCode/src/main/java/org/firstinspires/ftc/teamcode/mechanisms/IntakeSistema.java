package org.firstinspires.ftc.teamcode.mechanisms;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.core.RConstants;

public class IntakeSistema {

    private Servo feederServo;

    private boolean pushing = false;
    private long pushStartTime = 0;

    public void init(HardwareMap hardwareMap) {
        feederServo = hardwareMap.get(Servo.class, RConstants.FEEDER);
        rest();
    }

    public void update() {
        if (pushing) {
            long elapsed = System.currentTimeMillis() - pushStartTime;

            if (elapsed >= RConstants.INTAKE_PUSH_TIME_MS) {
                rest();
                pushing = false;
            }
        }
    }

    public void pushOne() {
        if (pushing) {
            return;
        }

        feederServo.setPosition(RConstants.INTAKE_PUSH_POSITION);
        pushStartTime = System.currentTimeMillis();
        pushing = true;
    }

    public void rest() {
        feederServo.setPosition(RConstants.INTAKE_REST_POSITION);
    }

    public boolean isBusy() {
        return pushing;
    }
}