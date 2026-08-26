package org.firstinspires.ftc.teamcode.mechanisms;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.teamcode.core.RConstants;
import org.firstinspires.ftc.teamcode.util.MathU;

public class ShooterSistema {

    private DcMotorEx shooterMotorLeft;
    private DcMotorEx shooterMotorRight;

    private double targetRPM = 0.0;

    public void init(HardwareMap hardwareMap) {
        shooterMotorLeft = hardwareMap.get(
                DcMotorEx.class,
                RConstants.SHOOTER_LEFT
        );

        shooterMotorRight = hardwareMap.get(
                DcMotorEx.class,
                RConstants.SHOOTER_RIGHT
        );

        // Ajuste conforme a montagem física do shooter.
        shooterMotorLeft.setDirection(DcMotorSimple.Direction.FORWARD);
        shooterMotorRight.setDirection(DcMotorSimple.Direction.REVERSE);

        configureMotor(shooterMotorLeft);
        configureMotor(shooterMotorRight);
    }

    private void configureMotor(DcMotorEx motor) {
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
    }

    public void setRPM(double rpm) {
        targetRPM = MathU.clamp(
                rpm,
                RConstants.MIN_SHOOTER_RPM,
                RConstants.MAX_SHOOTER_RPM
        );

        double ticksPerSecond = rpmToTicksPerSecond(targetRPM);

        shooterMotorLeft.setVelocity(ticksPerSecond);
        shooterMotorRight.setVelocity(ticksPerSecond);
    }

    public void setRawPower(double power) {
        double safePower = MathU.clamp(
                power,
                RConstants.MIN_MOTOR_POWER,
                RConstants.MAX_MOTOR_POWER
        );

        targetRPM = 0.0;

        shooterMotorLeft.setPower(safePower);
        shooterMotorRight.setPower(safePower);
    }

    public void stop() {
        targetRPM = 0.0;

        stopMotor(shooterMotorLeft);
        stopMotor(shooterMotorRight);
    }

    private void stopMotor(DcMotorEx motor) {
        if (motor != null) {
            motor.setVelocity(0.0);
            motor.setPower(0.0);
        }
    }

    public double getTargetRPM() {
        return targetRPM;
    }

    public double getLeftRPM() {
        return getMotorRPM(shooterMotorLeft);
    }

    public double getRightRPM() {
        return getMotorRPM(shooterMotorRight);
    }

    /**
     * Retorna a média da velocidade dos dois motores.
     */
    public double getCurrentRPM() {
        return (getLeftRPM() + getRightRPM()) / 2.0;
    }

    private double getMotorRPM(DcMotorEx motor) {
        if (motor == null) {
            return 0.0;
        }

        // abs evita RPM negativo no motor cuja direção foi invertida.
        double ticksPerSecond = Math.abs(motor.getVelocity());

        return ticksPerSecondToRPM(ticksPerSecond);
    }

    /**
     * Só considera o shooter pronto quando os dois motores
     * estiverem dentro da tolerância.
     */
    public boolean isAtTargetRPM() {
        if (targetRPM <= 0.0) {
            return false;
        }

        double leftError = Math.abs(getLeftRPM() - targetRPM);
        double rightError = Math.abs(getRightRPM() - targetRPM);

        return leftError <= RConstants.SHOOTER_READY_TOLERANCE_RPM
                && rightError <= RConstants.SHOOTER_READY_TOLERANCE_RPM;
    }

    private double rpmToTicksPerSecond(double rpm) {
        return (
                rpm
                        * RConstants.SHOOTER_TICKS_PER_REV
                        * RConstants.SHOOTER_GEAR_RATIO
        ) / 60.0;
    }

    private double ticksPerSecondToRPM(double ticksPerSecond) {
        return (ticksPerSecond * 60.0) / (
                RConstants.SHOOTER_TICKS_PER_REV
                        * RConstants.SHOOTER_GEAR_RATIO
        );
    }
}