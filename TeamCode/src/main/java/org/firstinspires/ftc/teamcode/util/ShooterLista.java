package org.firstinspires.ftc.teamcode.util;

import org.firstinspires.ftc.teamcode.core.RConstants;

public class ShooterLista {

    /*
     * Tabela de distância para RPM.
     *
     * A lógica é:
     * distância do shooter até o alvo em cm -> RPM necessário para acertar.
     *
     * A distância vem do range da AprilTag (via CalcDistAlvo). Com a torreta
     * removida (shooter fixo, sem ajuste fino de ângulo), os valores abaixo
     * são apenas um PONTO DE PARTIDA.
     *
     * TODO (calibração obrigatória no robô real, com o shooter fixo):
     *   1. Posicionar o robô a cada distância da tabela (medindo com a
     *      própria câmera/AprilTag, não fita métrica, para bater com
     *      o valor que o código vai ler em partida).
     *   2. Testar RPMs até acertar o alvo de forma consistente (várias
     *      bolinhas seguidas) e anotar o valor que funcionou.
     *   3. Substituir os valores de RPM_VALUES abaixo pelos medidos.
     *   4. Adicionar pontos extras se o alcance de jogo cobrir distâncias
     *      fora do intervalo 70cm-190cm testado aqui.
     */

    private static final double[] DISTANCES_CM = {
            70.0, 90.0, 110.0, 130.0, 150.0, 170.0, 190.0
    };

    private static final double[] RPM_VALUES = {
            1200.0, 1450.0, 1700.0, 1950.0, 2200.0, 2450.0, 2700.0
    };

    public double getRPMForDistance(double distanceCm) {
        if (!isTableValid()) {
            return RConstants.DEFAULT_SHOOTER_RPM;
        }

        if (distanceCm <= DISTANCES_CM[0]) {
            return clampRPM(RPM_VALUES[0]);
        }

        int lastIndex = DISTANCES_CM.length - 1;

        if (distanceCm >= DISTANCES_CM[lastIndex]) {
            return clampRPM(RPM_VALUES[lastIndex]);
        }

        for (int i = 0; i < DISTANCES_CM.length - 1; i++) {
            double distanceA = DISTANCES_CM[i];
            double distanceB = DISTANCES_CM[i + 1];

            if (distanceCm >= distanceA && distanceCm <= distanceB) {
                double rpmA = RPM_VALUES[i];
                double rpmB = RPM_VALUES[i + 1];

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
        return DISTANCES_CM.length > 0 && DISTANCES_CM.length == RPM_VALUES.length;
    }
}