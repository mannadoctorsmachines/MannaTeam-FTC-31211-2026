package org.firstinspires.ftc.teamcode.util;

import org.firstinspires.ftc.teamcode.core.RConstants;

public class CalcDistAlvo {

    public double getCameraDistanceCm(double aprilTagRangeInches) {
        double safeRangeInches = Math.max(0.0, aprilTagRangeInches);

        return safeRangeInches * RConstants.INCH_TO_CM;
    }

    public double getShooterDistanceCm(double aprilTagRangeInches) {
        double cameraDistanceCm = getCameraDistanceCm(aprilTagRangeInches);

        double shooterDistanceCm =
                cameraDistanceCm
                        + RConstants.CAMERA_TO_SHOOTER_OFFSET_CM
                        + RConstants.TAG_TO_TARGET_OFFSET_CM;

        return Math.max(0.0, shooterDistanceCm);
    }

    /**
     * Distância usada no autônomo quando o robô parte de uma posição conhecida
     * e se desloca em linha reta na direção do gol usando os encoders.
     */
    public double getShooterDistanceFromEncoderCm(
            double startDistanceToGoalCm,
            double traveledTowardGoalCm
    ) {
        double safeStartDistance = Math.max(0.0, startDistanceToGoalCm);
        double safeTraveledDistance = Math.max(0.0, traveledTowardGoalCm);

        return Math.max(0.0, safeStartDistance - safeTraveledDistance);
    }
}
