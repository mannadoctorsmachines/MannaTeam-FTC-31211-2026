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
    public static final String SHOOTER_LEFT = "shooterMotor1";
    public static final String SHOOTER_RIGHT = "shooterMotor2";
    public static final String FEEDER = "intakeMotor";

    // Sistemas ativos no robô; alterar o valor para true quando tiver a expansion hub
    public static final boolean USE_CAMERA = true;
    public static final boolean USE_SHOOTER = true;
    public static final boolean USE_FEEDER = true;

    // Observação: a torreta (CameraTurret) foi removida do robô.
    // O shooter agora é fixo. No TeleOp, a câmera fornece bearing e distância.
    // Nos autônomos, o chassi gira pela IMU e se posiciona pelos encoders;
    // a distância de cada posição de tiro é escrita diretamente na rota.
    // A versão antiga com torreta continua arquivada em backups/CameraTurret.java
    // e também deve ser mantida numa branch/tag separada no GitHub.

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

    // Shooter
    public static final double SHOOTER_TICKS_PER_REV = 537.7;
    public static final double SHOOTER_GEAR_RATIO = 1.0;

    public static final double MIN_SHOOTER_RPM = 300.0;
    public static final double MAX_SHOOTER_RPM = 6000.0;

    public static final double DEFAULT_SHOOTER_RPM = 5000.0;
    public static final double SHOOTER_READY_TOLERANCE_RPM = 85.0;

    // Ao segurar o left bumper, o shooter liga imediatamente e o intake
    // só é liberado depois deste tempo mínimo de aceleração.
    public static final long SHOOTER_SPINUP_DELAY_MS = 2000;

    // Intake / Feeder
    public static final double INTAKE_REST_POWER = 0.0;
    public static final double INTAKE_PUSH_POWER = 0.75;
    public static final long INTAKE_PUSH_TIME_MS = 260;

    // Conversões
    public static final double INCH_TO_CM = 2.54;

    // Cálculo de distância do shooter (medida pela câmera, via range da AprilTag)
    // Offset entre a câmera e o ponto de saída da bolinha no shooter.
    public static final double CAMERA_TO_SHOOTER_OFFSET_CM = 12.0;
    public static final double TAG_TO_TARGET_OFFSET_CM = 0.0;
    public static final double TURN_TOLERANCE_DEGREES = 2.0;

    // ID da AprilTag colocada no gol. Use -1 para aceitar qualquer tag visível.
    public static final int TARGET_APRIL_TAG_ID = -1;
    public static final long CAMERA_SOLUTION_MAX_AGE_MS = 500;

    // ---------------------------------------------------------------------
    // AUTÔNOMOS - limites gerais e tempos de segurança
    // ---------------------------------------------------------------------
    public static final long AUTO_DRIVE_TIMEOUT_MS = 4000;
    public static final long AUTO_TURN_TIMEOUT_MS = 3000;
    public static final long AUTO_SHOOTER_READY_TIMEOUT_MS = 4000;
    public static final long AUTO_DELAY_BETWEEN_SHOTS_MS = 500;
    public static final boolean AUTO_SHOOT_AFTER_RPM_TIMEOUT = true;

    // Mira
    public static final double AIM_TOLERANCE_DEGREES = 2.0;

    // Tempo máximo (ms) que o autônomo espera o robô girar em direção ao alvo
    public static final long AIM_TURN_TIMEOUT_MS = 1500;


}
