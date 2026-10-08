package org.firstinspires.ftc.teamcode.config;

/**
 * Escolhas de COMPOSIÇÃO do robô (quais implementações o Robot monta).
 * Parâmetros de cada componente ficam na Config do próprio componente.
 */
public class RobotConfig {

    public enum VisionProvider {
        APRILTAG_CAMERA,
        LIMELIGHT
    }

    // Fonte de visão usada pelo Robot.initVision().
    // APRILTAG_CAMERA = comportamento anterior do projeto.
    public static final VisionProvider VISION_PROVIDER = VisionProvider.APRILTAG_CAMERA;
}
