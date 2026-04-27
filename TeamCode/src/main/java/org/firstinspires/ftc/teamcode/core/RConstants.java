package org.firstinspires.ftc.teamcode.core;

public class RConstants {

    public static final String LEFT_DRIVE = "rodaEsquerda";
    public static final String RIGHT_DRIVE = "rodaDireita";

    public static final String CAMERA_TURRET = "cameraMotor";
    public static final String SHOOTER = "shooterMotor";
    public static final String FEEDER = "intakeServo";
    public static final String WEBCAM = "Webcam 1";

    public static final double MIN_MOTOR_POWER = -1.0; // limite dos motores
    public static final double MAX_MOTOR_POWER = 1.0; // limite dos motores

    public static final double DRIVE_POWER_SLOW = 0.45;
    public static final double DRIVE_POWER_NORMAL = 0.75;
    public static final double DRIVE_POWER_TURBO = 1.0;

    public static final double TICKS_PER_MOTOR_REV = 537.7;
    public static final double WHEEL_DIAMETER_CM = 9.6;
    public static final double DRIVE_GEAR_RATIO = 1.0;
    public static final double TICKS_PER_CM =
            (TICKS_PER_MOTOR_REV * DRIVE_GEAR_RATIO) / (Math.PI * WHEEL_DIAMETER_CM);
    public static final double TURN_KP = 0.018;
    public static final double TURN_KI = 0.0;
    public static final double TURN_KD = 0.0015;

    public static final double TURRET_KP = 0.018;
    public static final double TURRET_KI = 0.0;
    public static final double TURRET_KD = 0.002;

    public static final double TURRET_DEADBAND_DEGREES = 1.5;
    public static final double TURRET_MIN_POWER = 0.05;
    public static final double TURRET_MAX_POWER = 0.35;
    public static final double TURRET_SEARCH_POWER = 0.10;

    // Exemplo de uso: GoBILDA 5203 312 RPM usa 537.7 ticks/rev.
    public static final double SHOOTER_TICKS_PER_REV = 537.7;
    public static final double SHOOTER_GEAR_RATIO = 1.0;

    public static final double MIN_SHOOTER_RPM = 900.0;
    public static final double MAX_SHOOTER_RPM = 4200.0;

    public static final double DEFAULT_SHOOTER_RPM = 1900.0;
    public static final double SHOOTER_READY_TOLERANCE_RPM = 85.0;

    public static final double INTAKE_REST_POSITION = 0.15;
    public static final double INTAKE_PUSH_POSITION = 0.65;
    public static final long INTAKE_PUSH_TIME_MS = 260;

    public static final double INCH_TO_CM = 2.54;

    /*
     * Se a câmera está NA FRENTE do shooter, use positivo.
     * Se a câmera está ATRÁS do shooter, use negativo.
     */
    public static final double CAMERA_TO_SHOOTER_OFFSET_CM = 12.0;

    /*
     * Se a AprilTag não estiver exatamente no ponto onde vocês querem acertar,
     * compense aqui.
     */
    public static final double TAG_TO_TARGET_OFFSET_CM = 0.0;

    public static final double AIM_TOLERANCE_DEGREES = 2.0;
}