package org.firstinspires.ftc.teamcode.core;

public class RConstants {

    // Hardware Map - Drive
    public static final String LEFT_DRIVE = "rodaEsquerda";
    public static final String LEFTB_DRIVE = "trasEsquerda";
    public static final String RIGHT_DRIVE = "rodaDireita";
    public static final String RIGHTB_DRIVE = "trasDireita";

    // Hardware Map - Sensores
    public static final String IMU = "imu";
    public static final String WEBCAM = "Webcam 1";

    // Hardware Map - Mecanismos
    public static final String CAMERA_TURRET = "cameraMotor";
    public static final String SHOOTER = "shooterMotor";
    public static final String FEEDER = "intakeMotor";

    // Sistemas ativos no robô; alterar o valor para true quando tiver a expansion hub
    public static final boolean USE_CAMERA = false;
    public static final boolean USE_TURRET = false;
    public static final boolean USE_SHOOTER = false;
    public static final boolean USE_FEEDER = false;

    // Limites gerais
    public static final double MIN_MOTOR_POWER = -1.0;
    public static final double MAX_MOTOR_POWER = 1.0;

    // Drive
    public static final double DRIVE_POWER_SLOW = 0.45;
    public static final double DRIVE_POWER_NORMAL = 0.75;
    public static final double DRIVE_POWER_TURBO = 1.0;

    // Encoder do drive
    public static final double TICKS_PER_MOTOR_REV = 529.2;
    public static final double WHEEL_DIAMETER_CM = 7.5;
    public static final double DRIVE_GEAR_RATIO = 1.0;
    public static final double TICKS_PER_CM =
            (TICKS_PER_MOTOR_REV * DRIVE_GEAR_RATIO) / (Math.PI * WHEEL_DIAMETER_CM);

    // PID de giro do robô
    public static final double TURN_KP = 0.018;
    public static final double TURN_KI = 0.0;
    public static final double TURN_KD = 0.0015;

    // PID da torreta
    public static final double TURRET_KP = 0.018;
    public static final double TURRET_KI = 0.0;
    public static final double TURRET_KD = 0.002;

    public static final double TURRET_DEADBAND_DEGREES = 1.5;
    public static final double TURRET_MIN_POWER = 0.05;
    public static final double TURRET_MAX_POWER = 0.35;
    public static final double TURRET_SEARCH_POWER = 0.10;

    // Shooter
    public static final double SHOOTER_TICKS_PER_REV = 537.7;
    public static final double SHOOTER_GEAR_RATIO = 1.0;

    public static final double MIN_SHOOTER_RPM = 900.0;
    public static final double MAX_SHOOTER_RPM = 4200.0;

    public static final double DEFAULT_SHOOTER_RPM = 1900.0;
    public static final double SHOOTER_READY_TOLERANCE_RPM = 85.0;

    // Intake / Feeder
    public static final double INTAKE_REST_POWER = 0.0;
    public static final double INTAKE_PUSH_POWER = 0.65;
    public static final long INTAKE_PUSH_TIME_MS = 260;

    // Conversões
    public static final double INCH_TO_CM = 2.54;

    // Cálculo de distância do shooter
    public static final double CAMERA_TO_SHOOTER_OFFSET_CM = 12.0;
    public static final double TAG_TO_TARGET_OFFSET_CM = 0.0;

    // Mira
    public static final double AIM_TOLERANCE_DEGREES = 2.0;


}