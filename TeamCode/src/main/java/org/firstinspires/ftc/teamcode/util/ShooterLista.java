package org.firstinspires.ftc.teamcode.util;

import org.firstinspires.ftc.teamcode.core.RConstants;

public class ShooterLista {

    /*
     * Tabela de distância para RPM.
     *
     * A lógica é:
     * distância do shooter até o alvo em cm -> RPM necessário para acertar.
     *
     * Esses valores são apenas base.
     * O ideal é testar no robô real e ajustar cada ponto da tabela.
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