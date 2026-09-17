package org.firstinspires.ftc.teamcode.util;

import org.firstinspires.ftc.teamcode.robot.RConstants;

public class CalcDistAlvo {

    private boolean hasFilteredDistance = false;
    private double filteredShooterDistanceCm = 0.0;

    public double getCameraDistanceCm(double aprilTagRangeInches) {
        double safeRangeInches = Math.max(0.0, aprilTagRangeInches);

        // Valor bruto fornecido pela AprilTag.
        return safeRangeInches * RConstants.INCH_TO_CM;
    }

    public double getCalibratedCameraDistanceCm(double aprilTagRangeInches) {
        double rawCameraDistanceCm =
                getCameraDistanceCm(aprilTagRangeInches);

        return Math.max(
                0.0,
                rawCameraDistanceCm
                        * RConstants.CAMERA_DISTANCE_SCALE
                        + RConstants.CAMERA_DISTANCE_BIAS_CM
        );
    }

    public double getShooterDistanceCm(double aprilTagRangeInches) {
        double calibratedCameraDistanceCm =
                getCalibratedCameraDistanceCm(aprilTagRangeInches);

        double shooterDistanceCm =
                calibratedCameraDistanceCm
                        + RConstants.CAMERA_TO_SHOOTER_OFFSET_CM
                        + RConstants.TAG_TO_TARGET_OFFSET_CM;

        return Math.max(0.0, shooterDistanceCm);
    }

    /**
     * Atualiza a distância suavizada usada pelo autoshooter. A primeira leitura
     * válida entra inteira; as seguintes usam um filtro exponencial simples.
     */
    public double updateFilteredShooterDistanceCm(double aprilTagRangeInches) {
        double measuredDistanceCm = getShooterDistanceCm(aprilTagRangeInches);

        if (!hasFilteredDistance) {
            filteredShooterDistanceCm = measuredDistanceCm;
            hasFilteredDistance = true;
            return filteredShooterDistanceCm;
        }

        double alpha = MathU.clamp(
                RConstants.CAMERA_DISTANCE_FILTER_ALPHA,
                0.0,
                1.0
        );

        filteredShooterDistanceCm =
                alpha * measuredDistanceCm
                        + (1.0 - alpha) * filteredShooterDistanceCm;

        return filteredShooterDistanceCm;
    }

    /** Reinicia o filtro após perder a AprilTag ou trocar de alvo. */
    public void resetDistanceFilter() {
        hasFilteredDistance = false;
        filteredShooterDistanceCm = 0.0;
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
