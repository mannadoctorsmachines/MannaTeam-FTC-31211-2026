package org.firstinspires.ftc.teamcode.vision.limelight;

/** Configuração da Limelight 3A. */
public class LimelightConfig {

    // Nome do dispositivo no Driver Hub (valor do sample SensorLimelight3A;
    // CONFIRME no Driver Hub do robô).
    public static final String DEVICE_NAME = "limelight";

    // Pipeline usado para AprilTags (índice do sample; confirme no painel
    // da Limelight).
    public static final int PIPELINE_INDEX = 0;

    // Publicar a pose do robô (botpose) como PoseObservation?
    // false até que o sistema de coordenadas do botpose (origem no centro do
    // campo, em metros, segundo a Limelight) seja alinhado com o da
    // Localization e validado.
    public static final boolean PUBLISH_BOTPOSE = false;

    // A Limelight informa o botpose em metros.
    public static final double BOTPOSE_METERS_TO_CM = 100.0;
}
