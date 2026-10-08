package org.firstinspires.ftc.teamcode.localization;

/**
 * Configuração geral da Localization (independente do sensor).
 * Parâmetros do sensor ficam na config da implementação
 * (ver GobildaOdometryConfig).
 */
public class LocalizationConfig {

    // Aceitar correção de pose vinda de Vision (AprilTag/Limelight)?
    // Mantenha false até que o sistema de coordenadas da Vision esteja
    // alinhado com o da Localization e validado no robô.
    public static final boolean ACCEPT_VISION_CORRECTIONS = false;

    // Idade máxima de uma observação para ser aceita como correção.
    // PROVISÓRIO: não calibrado; sem efeito enquanto ACCEPT_VISION_CORRECTIONS
    // for false.
    public static final long MAX_OBSERVATION_AGE_MS = 150;
}
