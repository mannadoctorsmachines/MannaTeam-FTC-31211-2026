package org.firstinspires.ftc.teamcode.subsystems.intake;

/** Configuração do Intake / Feeder. */
public class IntakeConfig {

    // Hardware Map - Intake
    public static final String FEEDER = "intakeMotor";

    // Potências
    public static final double INTAKE_REST_POWER = 0.0;
    public static final double INTAKE_PUSH_POWER = 0.75;
    public static final double INTAKE_REVERSE_POWER = -0.75;

    // Duração da alimentação temporizada (pushOne)
    public static final long INTAKE_PUSH_TIME_MS = 260;
}
