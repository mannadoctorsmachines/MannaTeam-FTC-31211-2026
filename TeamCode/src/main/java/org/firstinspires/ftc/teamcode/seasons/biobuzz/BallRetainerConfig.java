package org.firstinspires.ftc.teamcode.seasons.biobuzz;

/**
 * Configuração da rampa de retenção da bola (servo).
 *
 * TODOS os valores físicos abaixo são PENDENTES DE CALIBRAÇÃO / TESTE FÍSICO.
 * Enquanto uma posição for Double.NaN, o mecanismo é considerado "não
 * configurado" e NÃO move o servo.
 */
public class BallRetainerConfig {

    // Liga o mecanismo na composição (BiobuzzRobot). Ative só depois de
    // preencher o nome do servo no Driver Hub e as duas posições.
    public static final boolean ENABLED = false;

    // PENDENTE: nome do servo no Driver Hub (confirmar/ajustar).
    public static final String SERVO = "retainerServo";

    // PENDENTE: posições do servo (0.0 a 1.0) medidas no robô.
    public static final double BLOCK_POSITION = Double.NaN;
    public static final double RELEASE_POSITION = Double.NaN;
}
