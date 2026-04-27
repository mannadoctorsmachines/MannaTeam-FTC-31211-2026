package org.firstinspires.ftc.teamcode.util;

import org.firstinspires.ftc.teamcode.core.RConstants;
import org.firstinspires.ftc.teamcode.core.RConstants;

public class ShooterLista {

    /*
     * Tabela exemplo.
     * Vocês precisam testar no robô real e ajustar.
     *
     * distância do shooter até o alvo em cm -> RPM que acerta
     */
    private final double[] distancesCm = {
            70.0, 90.0, 110.0, 130.0, 150.0, 170.0, 190.0
    };

    private final double[] rpmValues = {
            1200.0, 1450.0, 1700.0, 1950.0, 2200.0, 2450.0, 2700.0
    };

    public double getRPMForDistance(double distanceCm) {
        if (distanceCm <= distancesCm[0]) {
            return clampRPM(rpmValues[0]);
        }

        if (distanceCm >= distancesCm[distancesCm.length - 1]) {
            return clampRPM(rpmValues[rpmValues.length - 1]);
        }

        for (int i = 0; i < distancesCm.length - 1; i++) {
            double distanceA = distancesCm[i];
            double distanceB = distancesCm[i + 1];

            if (distanceCm >= distanceA && distanceCm <= distanceB) {
                double rpmA = rpmValues[i];
                double rpmB = rpmValues[i + 1];

                double percent = (distanceCm - distanceA) / (distanceB - distanceA);
                double interpolatedRPM = rpmA + percent * (rpmB - rpmA);

                return clampRPM(interpolatedRPM);
            }
        }

        return RConstants.DEFAULT_SHOOTER_RPM;
    }

    private double clampRPM(double rpm) {
        return MathU.clamp(
                rpm,
                RConstants.MIN_SHOOTER_RPM,
                RConstants.MAX_SHOOTER_RPM
        );
    }
}