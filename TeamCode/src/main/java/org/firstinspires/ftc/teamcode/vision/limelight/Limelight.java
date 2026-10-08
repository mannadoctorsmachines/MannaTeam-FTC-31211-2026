package org.firstinspires.ftc.teamcode.vision.limelight;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.teamcode.localization.Pose;
import org.firstinspires.ftc.teamcode.localization.PoseObservation;
import org.firstinspires.ftc.teamcode.vision.Vision;
import org.firstinspires.ftc.teamcode.vision.VisionConfig;

/**
 * Implementação de {@link Vision} com a Limelight 3A (fiduciais/AprilTags).
 * Única classe que conhece Limelight3A / LLResult.
 *
 * Usa somente APIs presentes no sample SensorLimelight3A do projeto:
 * pipelineSwitch, start, stop, getLatestResult, isValid, getFiducialResults,
 * getFiducialId, getTargetXDegrees, getBotpose e Pose3D.getPosition()/
 * getOrientation() (este último também usado em ConceptAprilTagLocalization).
 *
 * LIMITAÇÕES (nada inventado):
 *  - Range, yaw, forward e side NÃO são fornecidos: hasRange() é false e os
 *    getters devolvem 0.0. Medir distância com a Limelight exige a pose do
 *    alvo no espaço da câmera, API que não consta nos samples do projeto.
 *  - Com várias tags visíveis, usa a primeira que passa pelo filtro de ID
 *    (sem range não há como escolher a mais próxima).
 *  - bearing = -tx, para seguir a convenção da Vision (positivo = esquerda).
 *    A Limelight define tx positivo para a direita. Validar no robô.
 */
public class Limelight implements Vision {

    private static final String SOURCE_NAME = "limelight";

    private Limelight3A limelight;

    private LLResultTypes.FiducialResult target = null;
    private PoseObservation poseObservation = null;
    private long lastObservationTimeMs = -1;

    @Override
    public void init(HardwareMap hardwareMap) {
        limelight = hardwareMap.get(Limelight3A.class, LimelightConfig.DEVICE_NAME);
        limelight.pipelineSwitch(LimelightConfig.PIPELINE_INDEX);
        limelight.start();
    }

    @Override
    public void update() {
        target = null;
        poseObservation = null;

        if (limelight == null) {
            return;
        }

        LLResult result = limelight.getLatestResult();

        if (result == null || !result.isValid()) {
            return;
        }

        for (LLResultTypes.FiducialResult fiducial : result.getFiducialResults()) {
            if (VisionConfig.TARGET_APRIL_TAG_ID >= 0
                    && fiducial.getFiducialId() != VisionConfig.TARGET_APRIL_TAG_ID) {
                continue;
            }

            target = fiducial;
            break;
        }

        if (target == null) {
            return;
        }

        lastObservationTimeMs = System.currentTimeMillis();

        if (LimelightConfig.PUBLISH_BOTPOSE) {
            Pose3D botpose = result.getBotpose();

            if (botpose != null) {
                poseObservation = new PoseObservation(
                        new Pose(
                                botpose.getPosition().x * LimelightConfig.BOTPOSE_METERS_TO_CM,
                                botpose.getPosition().y * LimelightConfig.BOTPOSE_METERS_TO_CM,
                                botpose.getOrientation().getYaw(AngleUnit.DEGREES)
                        ),
                        SOURCE_NAME,
                        lastObservationTimeMs
                );
            }
        }
    }

    @Override
    public boolean hasTarget() {
        return target != null;
    }

    @Override
    public int getTargetId() {
        return target == null ? -1 : target.getFiducialId();
    }

    @Override
    public boolean hasRange() {
        return false;
    }

    @Override
    public double getRangeInches() {
        return 0.0;
    }

    @Override
    public double getBearingDegrees() {
        return target == null ? 0.0 : -target.getTargetXDegrees();
    }

    @Override
    public double getYawDegrees() {
        return 0.0;
    }

    @Override
    public double getForwardInches() {
        return 0.0;
    }

    @Override
    public double getSideInches() {
        return 0.0;
    }

    @Override
    public long getObservationAgeMs() {
        return lastObservationTimeMs < 0
                ? Long.MAX_VALUE
                : System.currentTimeMillis() - lastObservationTimeMs;
    }

    @Override
    public PoseObservation getPoseObservation() {
        return poseObservation;
    }

    @Override
    public void close() {
        if (limelight != null) {
            limelight.stop();
            limelight = null;
        }

        target = null;
        poseObservation = null;
    }
}
