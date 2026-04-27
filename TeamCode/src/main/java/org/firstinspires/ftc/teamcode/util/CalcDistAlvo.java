package org.firstinspires.ftc.teamcode.util;

import org.firstinspires.ftc.teamcode.core.RConstants;

public class CalcDistAlvo {

    public double getCameraDistanceCm(double aprilTagRangeInches) {
        return aprilTagRangeInches * RConstants.INCH_TO_CM;
    }

    public double getShooterDistanceCm(double aprilTagRangeInches) {
        double cameraDistanceCm = getCameraDistanceCm(aprilTagRangeInches);

        return cameraDistanceCm
                + RConstants.CAMERA_TO_SHOOTER_OFFSET_CM
                + RConstants.TAG_TO_TARGET_OFFSET_CM;
    }
}