package org.firstinspires.ftc.teamcode.util;

import org.firstinspires.ftc.teamcode.robot.RConstants;

public class ShooterLista {

    /*
     * Tabela de distância para RPM.
     *
     * A lógica é:
     * distância do shooter até o alvo em cm -> RPM DO ROLO necessário.
     *
     * No TeleOp a distância vem da AprilTag. Nos autônomos ela vem da posição
     * fixa calibrada e dos movimentos feitos por encoder. Com o shooter fixo,
     * os valores abaixo são apenas um PONTO DE PARTIDA.
     *
     * TODO (calibração obrigatória no robô real, com o shooter fixo):
     *   1. Posicionar o robô a cada distância da tabela. Para o TeleOp, compare
     *      com a leitura da câmera. Para o autônomo, use a posição marcada.
     *   2. Testar RPMs até acertar o alvo de forma consistente (várias
     *      bolinhas seguidas) e anotar o valor que funcionou.
     *   3. Substituir os pontos de CALIBRATION_POINTS abaixo pelos medidos.
     *   4. Adicionar pontos extras se o alcance de jogo cobrir distâncias
     *      fora do intervalo 70cm-190cm testado aqui.
     */
    // =====================================================================
    // >>> AJUSTE AQUI A RELAÇÃO DISTÂNCIA (cm) x RPM DO AUTOSHOOTER <<<
    // Cada linha possui exatamente: {distância em cm, RPM DO ROLO que funcionou}.
    // Mantenha as distâncias em ordem crescente.
    // =====================================================================
    private static final double[][] CALIBRATION_POINTS = {
            {70.0, 1100.0},
            {90.0, 1400.0},
            {110.0, 1600.0},
            {130.0, 1800.0},
            {150.0, 2100.0},
            {170.0, 2500.0},
            {190.0, 3000.0},
            {200.0, 3000.0},
            {220.0, 3000.0},
            {250.0, 3000.0},
            {280.0, 3000.0},
            {300.0, 3000.0},
            {320.0, 3000.0}
    };

    public double getRPMForDistance(double distanceCm) {
        if (!isTableValid()) {
            return RConstants.DEFAULT_SHOOTER_RPM;
        }

        if (distanceCm <= getDistanceAt(0)) {
            return clampRPM(getRPMAt(0));
        }

        int lastIndex = CALIBRATION_POINTS.length - 1;

        if (distanceCm >= getDistanceAt(lastIndex)) {
            return clampRPM(getRPMAt(lastIndex));
        }

        for (int i = 0; i < CALIBRATION_POINTS.length - 1; i++) {
            double distanceA = getDistanceAt(i);
            double distanceB = getDistanceAt(i + 1);

            if (distanceCm >= distanceA && distanceCm <= distanceB) {
                double rpmA = getRPMAt(i);
                double rpmB = getRPMAt(i + 1);

                return interpolateRPM(
                        distanceCm,
                        distanceA,
                        distanceB,
                        rpmA,
                        rpmB
                );
            }
        }

        return RConstants.DEFAULT_SHOOTER_RPM;
    }

    public double getMinimumCalibratedDistanceCm() {
        return CALIBRATION_POINTS.length == 0 ? 0.0 : getDistanceAt(0);
    }

    public double getMaximumCalibratedDistanceCm() {
        return CALIBRATION_POINTS.length == 0
                ? 0.0
                : getDistanceAt(CALIBRATION_POINTS.length - 1);
    }

    public boolean isDistanceInsideTable(double distanceCm) {
        return isTableValid()
                && distanceCm >= getMinimumCalibratedDistanceCm()
                && distanceCm <= getMaximumCalibratedDistanceCm();
    }

    private double interpolateRPM(
            double currentDistance,
            double distanceA,
            double distanceB,
            double rpmA,
            double rpmB
    ) {
        double percent = (currentDistance - distanceA) / (distanceB - distanceA);

        double interpolatedRPM = rpmA + percent * (rpmB - rpmA);

        return clampRPM(interpolatedRPM);
    }

    private double clampRPM(double rpm) {
        return MathU.clamp(
                rpm,
                RConstants.MIN_SHOOTER_RPM,
                RConstants.MAX_SHOOTER_RPM
        );
    }

    private boolean isTableValid() {
        if (CALIBRATION_POINTS.length == 0) {
            return false;
        }

        double previousDistance = -1.0;

        for (double[] point : CALIBRATION_POINTS) {
            if (point == null
                    || point.length != 2
                    || point[0] <= previousDistance
                    || point[1] <= 0.0) {
                return false;
            }

            previousDistance = point[0];
        }

        return true;
    }

    private double getDistanceAt(int index) {
        return CALIBRATION_POINTS[index][0];
    }

    private double getRPMAt(int index) {
        return CALIBRATION_POINTS[index][1];
    }
}
