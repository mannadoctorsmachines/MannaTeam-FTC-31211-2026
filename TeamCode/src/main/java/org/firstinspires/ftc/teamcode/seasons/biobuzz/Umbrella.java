package org.firstinspires.ftc.teamcode.seasons.biobuzz;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * "Guarda-chuva": recebe/posiciona a bola para o disparo e faz um movimento
 * de alavanca para a frente que ajuda a direcionar a bola.
 *
 * API puramente mecânica (repouso / frente): não conhece Shooter, Flower,
 * pontuação nem a sequência de disparo (isso é de um Command da Season).
 * O motor não é exposto.
 *
 * Usa RUN_TO_POSITION; ao iniciar fica com potência 0 (não se move até
 * receber um comando). Sem calibração completa em UmbrellaConfig, nenhuma
 * movimentação é feita.
 */
public class Umbrella {

    private DcMotorEx motor;

    public void init(HardwareMap hardwareMap) {
        motor = hardwareMap.get(DcMotorEx.class, UmbrellaConfig.MOTOR);

        if (!isConfigured()) {
            // Não toca no motor enquanto não houver valores calibrados.
            return;
        }

        motor.setDirection(UmbrellaConfig.DIRECTION);
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setTargetPosition(0);
        motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motor.setPower(0.0);
    }

    /** true se posições e potência já foram calibradas. */
    public boolean isConfigured() {
        return !Double.isNaN(UmbrellaConfig.REST_POSITION_TICKS)
                && !Double.isNaN(UmbrellaConfig.FORWARD_POSITION_TICKS)
                && !Double.isNaN(UmbrellaConfig.MOVE_POWER);
    }

    /** Move para a posição de repouso (recebe a bola). */
    public void moveToRest() {
        moveTo(UmbrellaConfig.REST_POSITION_TICKS);
    }

    /** Move para a frente (alavanca). */
    public void moveToForward() {
        moveTo(UmbrellaConfig.FORWARD_POSITION_TICKS);
    }

    public boolean isBusy() {
        return motor != null && isConfigured() && motor.isBusy();
    }

    /** Para o movimento. Seguro mesmo sem configuração. */
    public void stop() {
        if (motor != null && isConfigured()) {
            motor.setPower(0.0);
        }
    }

    private void moveTo(double ticks) {
        if (motor == null || !isConfigured()) {
            return;
        }

        motor.setTargetPosition((int) Math.round(ticks));
        motor.setPower(Math.abs(UmbrellaConfig.MOVE_POWER));
    }
}
