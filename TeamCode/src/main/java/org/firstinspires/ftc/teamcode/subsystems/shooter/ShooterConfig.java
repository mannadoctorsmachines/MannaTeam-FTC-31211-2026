package org.firstinspires.ftc.teamcode.subsystems.shooter;

/**
 * Configuração do Shooter. Todos os RPMs deste arquivo são RPM DO ROLO.
 *
 * Esta classe é a única fonte dos parâmetros físicos do shooter: o Shooter,
 * a ShooterLista e o OpMode de calibração leem os mesmos valores daqui.
 */
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

    // =====================================================================
    // >>> AJUSTE AQUI O RPM DO SHOOTER NORMAL / MANUAL (gamepad2 LB) <<<
    // =====================================================================
    // ATENÇÃO: Shooter.setRPM() limita o pedido a MIN_SHOOTER_RPM..MAX.
    // Com 20 RPM o shooter efetivamente roda em MIN_SHOOTER_RPM.
    public static final double MANUAL_SHOOTER_RPM = 20.0;

    // Fallback usado apenas se a tabela de distância estiver inválida.
    public static final double DEFAULT_SHOOTER_RPM = MANUAL_SHOOTER_RPM;

    // Ao ligar o shooter, o intake só é liberado depois deste tempo mínimo.
    public static final long SHOOTER_SPINUP_DELAY_MS = 2000;

    // Passos usados no OpMode de calibração de distância x RPM.
    public static final double SHOOTER_CALIBRATION_COARSE_STEP_RPM = 100.0;
    public static final double SHOOTER_CALIBRATION_FINE_STEP_RPM = 25.0;
}
