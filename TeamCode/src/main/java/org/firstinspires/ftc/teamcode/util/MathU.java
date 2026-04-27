package org.firstinspires.ftc.teamcode.util;

public class MathU {

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double aplicarZonaNeutra(double value, double deadband) {
        if (Math.abs(value) < deadband) {
            return 0.0;
        }

        return value;
    }

    public static double normalizarAngulo(double angle) {
        while (angle > 180.0) {
            angle -= 360.0;
        }

        while (angle < -180.0) {
            angle += 360.0;
        }

        return angle;
    }
}