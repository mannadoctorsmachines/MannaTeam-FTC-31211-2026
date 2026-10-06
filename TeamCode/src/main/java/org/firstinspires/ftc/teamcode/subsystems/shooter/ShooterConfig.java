package org.firstinspires.ftc.teamcode.subsystems.shooter;

public class ShooterConfig {

    // Hardware Map - Shooter
    public static final String SHOOTER_LEFT = "shooterMotor1";
    public static final String SHOOTER_RIGHT = "shooterMotor2";

    // Shooter motor / roller conversion
    public static final double SHOOTER_MOTOR_TICKS_PER_REV = 28.0;
    public static final double SHOOTER_MOTOR_NOMINAL_RPM = 6000.0;
    public static final double SHOOTER_MOTOR_REVS_PER_ROLLER_REV = 1.0;
    public static final double SHOOTER_TICKS_PER_ROLLER_REV =
            SHOOTER_MOTOR_TICKS_PER_REV * SHOOTER_MOTOR_REVS_PER_ROLLER_REV;

    // Shooter operating limits
    public static final double MIN_SHOOTER_RPM = 1000.0;
    public static final double MAX_SHOOTER_RPM = 6000.0;
    public static final double SHOOTER_READY_TOLERANCE_RPM = 75.0;
}
