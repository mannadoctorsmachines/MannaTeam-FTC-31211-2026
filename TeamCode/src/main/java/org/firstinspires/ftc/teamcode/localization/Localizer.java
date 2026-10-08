package org.firstinspires.ftc.teamcode.localization;

import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Contrato de Localization: estima e fornece a pose do robô.
 *
 * Independente do fornecedor do hardware: só a implementação (hoje
 * {@link GobildaOdometry}) conhece o driver do sensor. Não conhece regras de
 * jogo, Vision, Commands nem Pedro Pathing.
 *
 * Unidades, eixos e heading: ver {@link Pose}.
 *
 * Ciclo de vida:
 *  - Antes de init(): getPose() devolve Pose.ZERO e isPoseValid() é false.
 *  - init(): configura o sensor e define a origem (pose 0,0,0). O robô deve
 *    estar PARADO (o sensor recalibra o giroscópio).
 *  - update(): deve ser chamado uma vez por ciclo; quem faz isso é o
 *    Robot.update(), não cada OpMode.
 *  - isPoseValid(): true depois do init() e de pelo menos um update() ou
 *    setPose() bem-sucedido.
 */
public interface Localizer {

    void init(HardwareMap hardwareMap);

    /** Lê o sensor e atualiza a pose. */
    void update();

    /** Última pose calculada (nunca null). Não lê o hardware. */
    Pose getPose();

    boolean isPoseValid();

    double getXCm();

    double getYCm();

    double getHeadingDegrees();

    /**
     * Define a pose atual como (0, 0, 0). Não recalibra o giroscópio (isso só
     * acontece no init(), com o robô parado).
     */
    void resetPose();

    /** Define a pose atual (ex.: posição inicial conhecida do robô no campo). */
    void setPose(Pose pose);

    /**
     * Oferece uma pose observada por Vision como correção. A implementação
     * decide se aceita (ver LocalizationConfig). Substitui a pose; não faz
     * fusão de dados. Devolve true se a pose foi alterada.
     */
    boolean correctPose(PoseObservation observation);
}
