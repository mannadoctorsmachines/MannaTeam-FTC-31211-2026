package org.firstinspires.ftc.teamcode.seasons.biobuzz;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.robot.Robot;

/**
 * Composição da temporada BIOBUZZ: o Robot universal + os mecanismos
 * específicos desta temporada (rampa de retenção e guarda-chuva).
 *
 * Existe para que o Robot (núcleo universal) NÃO dependa de mecanismos da
 * temporada. Na próxima temporada, esta classe e este pacote são
 * substituídos; o Robot e os Subsystems universais permanecem.
 *
 * Mecanismos desligados (ENABLED = false na Config) não são criados e o
 * getter devolve null.
 */
public class BiobuzzRobot {

    private final Robot robot = new Robot();
    private final BallRetainer ballRetainer =
            BallRetainerConfig.ENABLED ? new BallRetainer() : null;
    private final Umbrella umbrella =
            UmbrellaConfig.ENABLED ? new Umbrella() : null;

    /** Ordem: Robot universal, depois mecanismos da temporada. */
    public void init(HardwareMap hardwareMap) {
        robot.init(hardwareMap);

        if (ballRetainer != null) {
            ballRetainer.init(hardwareMap);
        }

        if (umbrella != null) {
            umbrella.init(hardwareMap);
        }
    }

    /** Os mecanismos da temporada não precisam de update periódico. */
    public void update() {
        robot.update();
    }

    public void stopAll() {
        robot.stopAll();

        if (umbrella != null) {
            umbrella.stop();
        }
    }

    public Robot getRobot() {
        return robot;
    }

    /** Null se BallRetainerConfig.ENABLED for false. */
    public BallRetainer getBallRetainer() {
        return ballRetainer;
    }

    /** Null se UmbrellaConfig.ENABLED for false. */
    public Umbrella getUmbrella() {
        return umbrella;
    }
}
