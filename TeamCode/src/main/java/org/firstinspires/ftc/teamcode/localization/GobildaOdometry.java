package org.firstinspires.ftc.teamcode.localization;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;

/**
 * Implementação de {@link Localizer} com o goBILDA Pinpoint.
 *
 * É a ÚNICA classe do projeto que conhece GoBildaPinpointDriver. Dona do
 * Pinpoint: o Drivetrain não o inicializa nem o configura.
 */
public class GobildaOdometry implements Localizer {

    private GoBildaPinpointDriver pinpoint;

    private Pose pose = Pose.ZERO;
    private boolean initialized = false;
    private boolean poseValid = false;

    @Override
    public void init(HardwareMap hardwareMap) {
        pinpoint = hardwareMap.get(
                GoBildaPinpointDriver.class,
                GobildaOdometryConfig.PINPOINT
        );

        pinpoint.setOffsets(
                GobildaOdometryConfig.X_POD_OFFSET_CM,
                GobildaOdometryConfig.Y_POD_OFFSET_CM,
                DistanceUnit.CM
        );
        pinpoint.setEncoderResolution(GobildaOdometryConfig.POD_TYPE);
        pinpoint.setEncoderDirections(
                GobildaOdometryConfig.X_ENCODER_DIRECTION,
                GobildaOdometryConfig.Y_ENCODER_DIRECTION
        );

        // Zera posição e recalibra o giroscópio: robô parado.
        pinpoint.resetPosAndIMU();

        pose = Pose.ZERO;
        poseValid = false;
        initialized = true;
    }

    @Override
    public void update() {
        if (!initialized) {
            return;
        }

        pinpoint.update();

        Pose2D p = pinpoint.getPosition();
        pose = new Pose(
                p.getX(DistanceUnit.CM),
                p.getY(DistanceUnit.CM),
                p.getHeading(AngleUnit.DEGREES)
        );
        poseValid = true;
    }

    @Override
    public Pose getPose() {
        return pose;
    }

    @Override
    public boolean isPoseValid() {
        return initialized && poseValid;
    }

    @Override
    public double getXCm() {
        return pose.getXCm();
    }

    @Override
    public double getYCm() {
        return pose.getYCm();
    }

    @Override
    public double getHeadingDegrees() {
        return pose.getHeadingDegrees();
    }

    @Override
    public void resetPose() {
        setPose(Pose.ZERO);
    }

    @Override
    public void setPose(Pose newPose) {
        if (!initialized || newPose == null) {
            return;
        }

        pinpoint.setPosition(new Pose2D(
                DistanceUnit.CM,
                newPose.getXCm(),
                newPose.getYCm(),
                AngleUnit.DEGREES,
                newPose.getHeadingDegrees()
        ));

        pose = newPose;
        poseValid = true;
    }

    @Override
    public boolean correctPose(PoseObservation observation) {
        if (!LocalizationConfig.ACCEPT_VISION_CORRECTIONS
                || !initialized
                || observation == null
                || observation.getPose() == null
                || observation.getAgeMs() > LocalizationConfig.MAX_OBSERVATION_AGE_MS) {
            return false;
        }

        setPose(observation.getPose());
        return true;
    }
}
