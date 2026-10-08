package org.firstinspires.ftc.teamcode.vision;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.localization.PoseObservation;

/**
 * Contrato de Vision: fornece OBSERVAÇÕES. Não decide nada (não gira o
 * robô, não liga o shooter, não escolhe RPM) e não conhece regras de jogo.
 *
 * Implementações: AprilTagCamera (webcam + FTC Vision) e Limelight. Só elas
 * conhecem o SDK do fornecedor; quem compõe qual usar é o Robot
 * (RobotConfig.VISION_PROVIDER).
 *
 * Unidades (as mesmas que o projeto já usava na AprilTagCamera): distâncias
 * da observação em polegadas, ângulos em graus. Convenção de sinal:
 * bearing positivo = alvo à ESQUERDA da câmera (anti-horário), como no
 * ftcPose.bearing do FTC SDK.
 *
 * Campos que o fornecedor não consegue medir devolvem 0.0 e o método has...
 * correspondente devolve false.
 */
public interface Vision {

    void init(HardwareMap hardwareMap);

    /** Lê a observação mais recente. Chamado por Robot.update(). */
    void update();

    boolean hasTarget();

    int getTargetId();

    /** true se getRangeInches() é uma medida real. */
    boolean hasRange();

    double getRangeInches();

    double getBearingDegrees();

    double getYawDegrees();

    double getForwardInches();

    double getSideInches();

    /**
     * Milissegundos desde a última vez que um alvo foi visto.
     * Long.MAX_VALUE se nunca foi visto.
     */
    long getObservationAgeMs();

    /**
     * Pose do robô observada, já em unidades do projeto e no sistema de
     * coordenadas da Localization, ou null se o fornecedor não oferece isso
     * (ou se o alinhamento de coordenadas ainda não foi configurado).
     */
    PoseObservation getPoseObservation();

    void close();
}
