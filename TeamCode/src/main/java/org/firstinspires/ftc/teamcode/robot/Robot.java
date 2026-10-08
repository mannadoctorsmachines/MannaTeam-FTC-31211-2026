package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.config.RobotConfig;
import org.firstinspires.ftc.teamcode.localization.GobildaOdometry;
import org.firstinspires.ftc.teamcode.localization.Localizer;
import org.firstinspires.ftc.teamcode.localization.LocalizationConfig;
import org.firstinspires.ftc.teamcode.subsystems.drivetrain.Drivetrain;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.shooter.Shooter;
import org.firstinspires.ftc.teamcode.vision.Vision;
import org.firstinspires.ftc.teamcode.vision.apriltag.AprilTagCamera;
import org.firstinspires.ftc.teamcode.vision.limelight.Limelight;

/**
 * Ponto de composição do robô: cria, inicializa, atualiza e para os
 * componentes.
 *
 * O Robot NÃO controla motores, NÃO contém PID, NÃO contém lógica de
 * Shooter/Intake, NÃO calcula pose, NÃO processa AprilTags e NÃO conhece
 * estratégia nem regras da temporada.
 *
 * Componentes opcionais respeitam as flags de {@link RConstants}: quando
 * desligados não são criados e o getter devolve null.
 *
 * ORDEM DE INICIALIZAÇÃO (init): Drivetrain -> Localization -> Shooter ->
 * Intake. A Vision é iniciada à parte (initVision), porque nem todo OpMode a
 * usa. A fonte de visão vem de {@link RobotConfig#VISION_PROVIDER}.
 *
 * CICLO DE ATUALIZAÇÃO: o OpMode chama robot.update() uma vez por ciclo.
 * Ele atualiza Localization, Vision e Intake (temporizador do pushOne), nessa
 * ordem. Novos OpModes não precisam saber quais componentes têm update().
 * Em LinearOpMode, chame robot.update() dentro dos laços de espera.
 */
public class Robot {

    private final Drivetrain drivetrain;
    private final Shooter shooter;
    private final Intake intake;
    private final Localizer localization;
    private Vision vision;

    private boolean initialized = false;

    public Robot() {
        drivetrain = new Drivetrain();
        shooter = RConstants.USE_SHOOTER ? new Shooter() : null;
        intake = RConstants.USE_FEEDER ? new Intake() : null;
        localization = RConstants.USE_LOCALIZATION ? new GobildaOdometry() : null;
    }

    /** Inicializa drivetrain, localization, shooter e intake (conforme flags). */
    public void init(HardwareMap hardwareMap) {
        drivetrain.init(hardwareMap);

        if (localization != null) {
            localization.init(hardwareMap);
        }

        if (shooter != null) {
            shooter.init(hardwareMap);
        }

        if (intake != null) {
            intake.init(hardwareMap);
        }

        initialized = true;
    }

    /** Cria e inicializa a Vision escolhida em RobotConfig, se USE_CAMERA. */
    public void initVision(HardwareMap hardwareMap) {
        if (!RConstants.USE_CAMERA) {
            return;
        }

        switch (RobotConfig.VISION_PROVIDER) {
            case LIMELIGHT:
                vision = new Limelight();
                break;
            case APRILTAG_CAMERA:
            default:
                vision = new AprilTagCamera();
                break;
        }

        vision.init(hardwareMap);
    }

    /** Atualiza os componentes que dependem de ciclo. Uma vez por ciclo. */
    public void update() {
        if (!initialized) {
            return;
        }

        if (localization != null) {
            localization.update();
        }

        if (vision != null) {
            vision.update();

            // Fronteira Vision -> Localization. Só age se a correção estiver
            // habilitada em LocalizationConfig (desligada por padrão).
            if (localization != null && LocalizationConfig.ACCEPT_VISION_CORRECTIONS) {
                localization.correctPose(vision.getPoseObservation());
            }
        }

        if (intake != null) {
            intake.update();
        }
    }

    public Drivetrain getDrivetrain() {
        return drivetrain;
    }

    /** Null se USE_SHOOTER for false. */
    public Shooter getShooter() {
        return shooter;
    }

    /** Null se USE_FEEDER for false. */
    public Intake getIntake() {
        return intake;
    }

    /** Null se USE_LOCALIZATION for false. */
    public Localizer getLocalization() {
        return localization;
    }

    /** Null se USE_CAMERA for false ou se initVision() ainda não foi chamado. */
    public Vision getVision() {
        return vision;
    }

    /** Para drivetrain, intake e shooter. Seguro para chamar antes do init(). */
    public void stopAll() {
        if (!initialized) {
            return;
        }

        drivetrain.stop();

        if (intake != null) {
            intake.stop();
        }

        if (shooter != null) {
            shooter.stop();
        }
    }

    /** Libera a Vision. Seguro para chamar mais de uma vez. */
    public void closeVision() {
        if (vision != null) {
            vision.close();
        }
    }
}
