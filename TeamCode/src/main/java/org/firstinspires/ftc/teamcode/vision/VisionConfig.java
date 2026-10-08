package org.firstinspires.ftc.teamcode.vision;

/** Configuração da visão (webcam + AprilTag) e do cálculo de distância. */
public class VisionConfig {

    // Hardware Map - Visão
    public static final String WEBCAM = "Webcam 1";

    // ID da AprilTag colocada no gol. Use -1 para aceitar qualquer tag visível.
    public static final int TARGET_APRIL_TAG_ID = -1;

    // Idade máxima de uma leitura da câmera para ainda ser considerada atual.
    public static final long CAMERA_SOLUTION_MAX_AGE_MS = 500;

    // Tolerância de alinhamento com a AprilTag.
    public static final double AIM_TOLERANCE_DEGREES = 2.0;

    // Correção encontrada nos testes físicos da webcam.
    public static final double CAMERA_DISTANCE_SCALE = 0.78125;
    public static final double CAMERA_DISTANCE_BIAS_CM = 0.0;

    // CAMERA_TO_SHOOTER_OFFSET_CM corrige a diferença entre a câmera e o ponto
    // de saída da bolinha. Positivo se o shooter estiver mais longe do alvo
    // que a câmera; negativo se estiver mais perto.
    public static final double CAMERA_TO_SHOOTER_OFFSET_CM = -26.5; // 12.0 valor antigo
    public static final double TAG_TO_TARGET_OFFSET_CM = 0.0;

    // Suaviza pequenas oscilações da câmera. 1.0 = sem filtro; valores menores
    // deixam a leitura mais estável, porém um pouco mais lenta.
    public static final double CAMERA_DISTANCE_FILTER_ALPHA = 0.5; // 0.25 valor antigo
}
