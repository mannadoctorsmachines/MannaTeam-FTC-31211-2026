package org.firstinspires.ftc.teamcode.subsystems.drivetrain;

public class DrivetrainConfig {

    // Hardware Map - Drive
    public static final String LEFT_DRIVE = "rodaEsquerda";
    public static final String LEFTB_DRIVE = "trasEsquerda";
    public static final String RIGHT_DRIVE = "rodaDireita";
    public static final String RIGHTB_DRIVE = "trasDireita";

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
}
