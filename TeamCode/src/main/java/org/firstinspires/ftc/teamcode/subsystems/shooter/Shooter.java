package org.firstinspires.ftc.teamcode.subsystems.shooter;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.teamcode.robot.RConstants;
import org.firstinspires.ftc.teamcode.util.MathU;

public class Shooter {

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
        shooterMotorLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        shooterMotorRight.setDirection(DcMotorSimple.Direction.FORWARD);

        configureMotor(shooterMotorLeft);
        configureMotor(shooterMotorRight);
    }

    private void configureMotor(DcMotorEx motor) {
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
    }

    /**
     * Zera somente os encoders do shooter. Use com os motores parados no
     * OpMode de calibração para medir ticks por volta do rolo.
     */
    public void resetEncoders() {
        stop();
        configureMotor(shooterMotorLeft);
        configureMotor(shooterMotorRight);
    }

    public void setRPM(double rpm) {
        if (!hasValidEncoderConversion()
                || !hasNominalMotorRPMConfigured()) {
            stop();
            return;
        }

        double maximumAllowedRPM = getMaximumAllowedRollerRPM();
        double minimumAllowedRPM = Math.min(
                RConstants.MIN_SHOOTER_RPM,
                maximumAllowedRPM
        );

        targetRPM = MathU.clamp(rpm, minimumAllowedRPM, maximumAllowedRPM);

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
        return getRollerRPM(shooterMotorLeft);
    }

    public double getRightRPM() {
        return getRollerRPM(shooterMotorRight);
    }

    /**
     * Retorna a média da velocidade dos dois motores.
     */
    public double getCurrentRPM() {
        return (getLeftRPM() + getRightRPM()) / 2.0;
    }

    /** RPM real do eixo de saída do motor esquerdo. */
    public double getLeftMotorRPM() {
        return getMotorRPM(shooterMotorLeft);
    }

    /** RPM real do eixo de saída do motor direito. */
    public double getRightMotorRPM() {
        return getMotorRPM(shooterMotorRight);
    }

    /** Ticks por segundo crus do encoder esquerdo. */
    public double getLeftTicksPerSecond() {
        return getTicksPerSecond(shooterMotorLeft);
    }

    /** Ticks por segundo crus do encoder direito. */
    public double getRightTicksPerSecond() {
        return getTicksPerSecond(shooterMotorRight);
    }

    public int getLeftEncoderPosition() {
        return shooterMotorLeft == null
                ? 0
                : shooterMotorLeft.getCurrentPosition();
    }

    public int getRightEncoderPosition() {
        return shooterMotorRight == null
                ? 0
                : shooterMotorRight.getCurrentPosition();
    }

    private double getTicksPerSecond(DcMotorEx motor) {
        if (motor == null) {
            return 0.0;
        }

        // abs evita valor negativo no motor cuja direção foi invertida.
        return Math.abs(motor.getVelocity());
    }

    private double getMotorRPM(DcMotorEx motor) {
        if (RConstants.SHOOTER_MOTOR_TICKS_PER_REV <= 0.0) {
            return 0.0;
        }

        return (getTicksPerSecond(motor) * 60.0)
                / RConstants.SHOOTER_MOTOR_TICKS_PER_REV;
    }

    private double getRollerRPM(DcMotorEx motor) {
        if (!hasValidEncoderConversion()) {
            return 0.0;
        }

        return getMotorRPM(motor)
                / RConstants.SHOOTER_MOTOR_REVS_PER_ROLLER_REV;
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
        return rpm * RConstants.SHOOTER_TICKS_PER_ROLLER_REV / 60.0;
    }

    /**
     * Maior RPM do rolo que o código aceitará. Quando o RPM nominal do motor
     * estiver preenchido, o limite também respeita a transmissão das polias.
     */
    public double getMaximumAllowedRollerRPM() {
        double configuredMaximum = RConstants.MAX_SHOOTER_RPM;

        if (RConstants.SHOOTER_MOTOR_NOMINAL_RPM <= 0.0
                || RConstants.SHOOTER_MOTOR_REVS_PER_ROLLER_REV <= 0.0) {
            return configuredMaximum;
        }

        double nominalRollerRPM =
                RConstants.SHOOTER_MOTOR_NOMINAL_RPM
                        / RConstants.SHOOTER_MOTOR_REVS_PER_ROLLER_REV;

        return Math.min(configuredMaximum, nominalRollerRPM);
    }

    public double getNominalRollerRPM() {
        if (RConstants.SHOOTER_MOTOR_NOMINAL_RPM <= 0.0
                || RConstants.SHOOTER_MOTOR_REVS_PER_ROLLER_REV <= 0.0) {
            return 0.0;
        }

        return RConstants.SHOOTER_MOTOR_NOMINAL_RPM
                / RConstants.SHOOTER_MOTOR_REVS_PER_ROLLER_REV;
    }

    public boolean hasNominalMotorRPMConfigured() {
        return RConstants.SHOOTER_MOTOR_NOMINAL_RPM > 0.0;
    }

    public boolean hasValidEncoderConversion() {
        return RConstants.SHOOTER_MOTOR_TICKS_PER_REV > 0.0
                && RConstants.SHOOTER_MOTOR_REVS_PER_ROLLER_REV > 0.0
                && RConstants.SHOOTER_TICKS_PER_ROLLER_REV > 0.0;
    }
}
