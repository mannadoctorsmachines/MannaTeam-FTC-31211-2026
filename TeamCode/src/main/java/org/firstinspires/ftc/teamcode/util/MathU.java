package org.firstinspires.ftc.teamcode.util;

public class MathU {

    public static double clamp(double value, double min, double max) {
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }

        return Math.max(min, Math.min(max, value));
    }

    public static double aplicarZonaNeutra(double value, double deadband) {
        deadband = Math.abs(deadband);

        if (Math.abs(value) < deadband) {
            return 0.0;
        }

        return value;
    }

    public static double normalizarAngulo(double angle) {
        angle %= 360.0;

        if (angle > 180.0) {
            angle -= 360.0;
        }

        if (angle < -180.0) {
            angle += 360.0;
        }

        return angle;
    }
}