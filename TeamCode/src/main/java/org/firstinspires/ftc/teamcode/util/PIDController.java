package org.firstinspires.ftc.teamcode.util;

public class PIDController {

    private double kP;
    private double kI;
    private double kD;

    private double integral = 0.0;
    private double lastError = 0.0;

    private boolean firstRun = true;

    private double integralLimit = 1_000.0;

    public PIDController(double kP, double kI, double kD) {
        setPID(kP, kI, kD);
    }

    public double calculate(double target, double current) {
        double error = target - current;

        integral += error;
        integral = MathU.clamp(integral, -integralLimit, integralLimit);

        double derivative = 0.0;

        if (!firstRun) {
            derivative = error - lastError;
        }

        firstRun = false;
        lastError = error;

        return (kP * error) + (kI * integral) + (kD * derivative);
    }

    public void reset() {
        integral = 0.0;
        lastError = 0.0;
        firstRun = true;
    }

    public void setPID(double kP, double kI, double kD) {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;

        reset();
    }

    public void setIntegralLimit(double integralLimit) {
        this.integralLimit = Math.abs(integralLimit);
    }

    public double getLastError() {
        return lastError;
    }
}