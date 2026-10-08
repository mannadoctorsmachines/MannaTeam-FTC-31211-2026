package org.firstinspires.ftc.teamcode.seasons.biobuzz;

import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * Configuração do "guarda-chuva" (motor Core Hex).
 *
 * TODOS os valores físicos abaixo são PENDENTES DE CALIBRAÇÃO / TESTE FÍSICO.
 * Enquanto uma posição ou a potência for Double.NaN, o mecanismo é
 * considerado "não configurado" e NÃO move o motor.
 */
public class UmbrellaConfig {

    // Liga o mecanismo na composição (BiobuzzRobot). Ative só depois de
    // preencher nome, sentido, posições e potência.
    public static final boolean ENABLED = false;

    // PENDENTE: nome do motor no Driver Hub (confirmar/ajustar).
    public static final String MOTOR = "umbrellaMotor";

    // PENDENTE: sentido do motor.
    public static final DcMotorSimple.Direction DIRECTION = DcMotorSimple.Direction.FORWARD;

    // As posições são em ticks do encoder, contados a partir do INIT: o
    // mecanismo deve estar na posição de repouso quando o OpMode inicia
    // (o encoder é zerado no init). PENDENTE: valores medidos no robô.
    public static final double REST_POSITION_TICKS = Double.NaN;
    public static final double FORWARD_POSITION_TICKS = Double.NaN;

    // PENDENTE: potência máxima do movimento (0.0 a 1.0).
    public static final double MOVE_POWER = Double.NaN;
}
