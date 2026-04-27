package org.firstinspires.ftc.teamcode.util;

public class PIDController {

    private double kP;
    private double kI;
    private double kD;

    private double integral;
    private double lastError;
    private boolean firstRun = true;

    public PIDController(double kP, double kI, double kD) {
        setPID(kP, kI, kD);
    }

    public double calculate(double target, double current) {
        double error = target - current;

        integral += error;

        double derivative;
        if (firstRun) {
            derivative = 0.0;
            firstRun = false;
        } else {
            derivative = error - lastError;
        }

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
}