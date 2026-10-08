package org.firstinspires.ftc.teamcode.seasons.biobuzz;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

/**
 * Rampa de retenção: segura a bola depois do Intake e antes do Shooter, e a
 * libera quando a sequência de disparo pede.
 *
 * API puramente mecânica (block/release): não conhece Shooter, Flower,
 * pontuação nem a sequência de disparo (isso é de um Command da Season).
 * O servo não é exposto.
 */
public class BallRetainer {

    public enum State {
        /** Nenhuma posição foi comandada desde o init. */
        UNKNOWN,
        BLOCKING,
        RELEASED
    }

    private Servo servo;
    private State state = State.UNKNOWN;

    public void init(HardwareMap hardwareMap) {
        servo = hardwareMap.get(Servo.class, BallRetainerConfig.SERVO);
        state = State.UNKNOWN;
    }

    /** true se as duas posições já foram calibradas. */
    public boolean isConfigured() {
        return !Double.isNaN(BallRetainerConfig.BLOCK_POSITION)
                && !Double.isNaN(BallRetainerConfig.RELEASE_POSITION);
    }

    /** Segura a bola. Não faz nada se o mecanismo não estiver configurado. */
    public void block() {
        if (servo == null || !isConfigured()) {
            return;
        }

        servo.setPosition(BallRetainerConfig.BLOCK_POSITION);
        state = State.BLOCKING;
    }

    /** Libera a bola. Não faz nada se o mecanismo não estiver configurado. */
    public void release() {
        if (servo == null || !isConfigured()) {
            return;
        }

        servo.setPosition(BallRetainerConfig.RELEASE_POSITION);
        state = State.RELEASED;
    }

    public State getState() {
        return state;
    }
}
