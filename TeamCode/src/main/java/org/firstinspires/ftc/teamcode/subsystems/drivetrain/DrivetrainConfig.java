package org.firstinspires.ftc.teamcode.subsystems.drivetrain;

public class DrivetrainConfig {

    // Hardware Map - Drive
    public static final String LEFT_DRIVE = "rodaEsquerda";
    public static final String LEFTB_DRIVE = "trasEsquerda";
    public static final String RIGHT_DRIVE = "rodaDireita";
    public static final String RIGHTB_DRIVE = "trasDireita";

    // Hardware Map - Sensores usados pelo drivetrain
    public static final String IMU = "imu";

    // Drive
    public static final double DRIVE_POWER_SLOW = 0.45;
    public static final double DRIVE_POWER_NORMAL = 0.75;
    public static final double DRIVE_POWER_TURBO = 1.0;

    // Revolução

    public static final double TICKS_PER_MOTOR_REV = 28;
    public static final double WHEEL_DIAMETER_CM = 7.6;
    public static final double DRIVE_GEAR_RATIO = 5.0;
    public static final double TICKS_PER_CM =
            (TICKS_PER_MOTOR_REV * DRIVE_GEAR_RATIO) / (Math.PI * WHEEL_DIAMETER_CM);

    // Mecanum normalmente escorrega mais de lado. Comece com este fator e
    // calibre separadamente comandando um strafe de 100 cm.
    public static final double STRAFE_CORRECTION = 1.15;
    public static final double STRAFE_TICKS_PER_CM = TICKS_PER_CM * STRAFE_CORRECTION;

    // PID de giro do robô
    public static final double TURN_KP = 0.018;
    public static final double TURN_KI = 0.0;
    public static final double TURN_KD = 0.0015;
    public static final double TURN_MIN_POWER = 0.12;
    public static final double TURN_MAX_POWER = 0.45;
    public static final double TURN_TOLERANCE_DEGREES = 2.0;
}
