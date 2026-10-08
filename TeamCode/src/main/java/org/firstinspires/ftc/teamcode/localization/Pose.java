package org.firstinspires.ftc.teamcode.localization;

import org.firstinspires.ftc.teamcode.util.MathU;

/**
 * Pose do robô no plano, representação PRÓPRIA do projeto (independente de
 * Pinpoint, Pedro Pathing ou FTC SDK). Imutável.
 *
 * UNIDADES: centímetros (X, Y) e graus (heading). São as unidades que o
 * projeto já usa (andarCm, WHEEL_DIAMETER_CM, distâncias do shooter, IMU em
 * graus). Quem precisar de outra unidade (ex.: Pedro usa polegadas e
 * radianos) converte NA FRONTEIRA da integração, nunca aqui.
 *
 * CONVENÇÕES (iguais às do Pinpoint, conforme a documentação da goBILDA;
 * ainda NÃO validadas fisicamente neste robô):
 *  - X positivo = para a frente do robô na pose inicial;
 *  - Y positivo = para a esquerda do robô na pose inicial;
 *  - heading positivo = anti-horário, normalizado em [-180, 180];
 *  - heading 0 = direção em que o robô estava na origem da pose.
 */
public final class Pose {

    public static final Pose ZERO = new Pose(0.0, 0.0, 0.0);

    private final double xCm;
    private final double yCm;
    private final double headingDegrees;

    public Pose(double xCm, double yCm, double headingDegrees) {
        this.xCm = xCm;
        this.yCm = yCm;
        this.headingDegrees = MathU.normalizarAngulo(headingDegrees);
    }

    public double getXCm() {
        return xCm;
    }

    public double getYCm() {
        return yCm;
    }

    public double getHeadingDegrees() {
        return headingDegrees;
    }

    @Override
    public String toString() {
        return String.format("Pose(x=%.1f cm, y=%.1f cm, heading=%.1f deg)",
                xCm, yCm, headingDegrees);
    }
}
