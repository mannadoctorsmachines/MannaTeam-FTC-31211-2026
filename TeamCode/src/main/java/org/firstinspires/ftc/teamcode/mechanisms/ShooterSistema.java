package org.firstinspires.ftc.teamcode.mechanisms;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.core.RConstants;
import org.firstinspires.ftc.teamcode.util.MathU;

public class ShooterSistema {

    private DcMotorEx shooterMotor;
    private double targetRPM = 0.0;

    public void init(HardwareMap hardwareMap) {
        shooterMotor = hardwareMap.get(DcMotorEx.class, RConstants.SHOOTER);

        shooterMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooterMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Shooter geralmente fica melhor em FLOAT,
        // porque ele não tenta frear bruscamente quando para.
        shooterMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
    }

    public void setRPM(double rpm) {
        targetRPM = MathU.clamp(
                rpm,
                RConstants.MIN_SHOOTER_RPM,
                RConstants.MAX_SHOOTER_RPM
        );

        double ticksPerSecond = rpmToTicksPerSecond(targetRPM);
        shooterMotor.setVelocity(ticksPerSecond);
    }

    public void setRawPower(double power) {
        double safePower = MathU.clamp(
                power,
                RConstants.MIN_MOTOR_POWER,
                RConstants.MAX_MOTOR_POWER
        );

        targetRPM = 0.0;
        shooterMotor.setPower(safePower);
    }

    public void stop() {
        targetRPM = 0.0;

        if (shooterMotor != null) {
            shooterMotor.setVelocity(0.0);
            shooterMotor.setPower(0.0);
        }
    }

    public double getTargetRPM() {
        return targetRPM;
    }

    public double getCurrentRPM() {
        if (shooterMotor == null) {
            return 0.0;
        }

        double ticksPerSecond = shooterMotor.getVelocity();

        return ticksPerSecondToRPM(ticksPerSecond);
    }

    public boolean isAtTargetRPM() {
        if (targetRPM <= 0.0) {
            return false;
        }

        double errorRPM = Math.abs(getCurrentRPM() - targetRPM);

        return errorRPM <= RConstants.SHOOTER_READY_TOLERANCE_RPM;
    }

    private double rpmToTicksPerSecond(double rpm) {
        return (rpm * RConstants.SHOOTER_TICKS_PER_REV * RConstants.SHOOTER_GEAR_RATIO) / 60.0;
    }

    private double ticksPerSecondToRPM(double ticksPerSecond) {
        return (ticksPerSecond * 60.0) / (RConstants.SHOOTER_TICKS_PER_REV * RConstants.SHOOTER_GEAR_RATIO);
    }
}