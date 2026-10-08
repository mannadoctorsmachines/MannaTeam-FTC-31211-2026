package org.firstinspires.ftc.teamcode.vision.apriltag;

import org.firstinspires.ftc.teamcode.localization.PoseObservation;
import org.firstinspires.ftc.teamcode.vision.Vision;
import org.firstinspires.ftc.teamcode.vision.VisionConfig;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import android.util.Size;

import java.util.List;

/**
 * Implementação de {@link Vision} com webcam + FTC Vision (AprilTagProcessor).
 * Única classe que conhece VisionPortal / AprilTagProcessor.
 */
public class AprilTagCamera implements Vision {

    private AprilTagProcessor aprilTagProcessor;
    private VisionPortal visionPortal;

    private AprilTagDetection atualDetection;
    private long lastObservationTimeMs = -1;

    @Override
    public void init(HardwareMap hardwareMap) {
        aprilTagProcessor = new AprilTagProcessor.Builder()
                .setDrawAxes(true)
                .setDrawCubeProjection(true)
                .setDrawTagID(true)
                .setDrawTagOutline(true)
                .build();

        visionPortal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, VisionConfig.WEBCAM))
                .setCameraResolution(new Size(640, 480))
                .addProcessor(aprilTagProcessor)
                .build();
    }

    @Override
    public void update() {
        atualDetection = null;

        if (aprilTagProcessor == null) {
            return;
        }

        List<AprilTagDetection> detections = aprilTagProcessor.getDetections();

        if (detections == null || detections.isEmpty()) {
            return;
        }

        AprilTagDetection DetectionProxima = null;
        double RangeProximo = Double.MAX_VALUE;

        for (AprilTagDetection detection : detections) {
            if (detection.metadata == null || detection.ftcPose == null) {
                continue;
            }

            if (VisionConfig.TARGET_APRIL_TAG_ID >= 0
                    && detection.id != VisionConfig.TARGET_APRIL_TAG_ID) {
                continue;
            }

            if (detection.ftcPose.range < RangeProximo) {
                RangeProximo = detection.ftcPose.range;
                DetectionProxima = detection;
            }
        }

        atualDetection = DetectionProxima;

        if (atualDetection != null) {
            lastObservationTimeMs = System.currentTimeMillis();
        }
    }

    @Override
    public boolean hasRange() {
        return hasTarget();
    }

    @Override
    public long getObservationAgeMs() {
        return lastObservationTimeMs < 0
                ? Long.MAX_VALUE
                : System.currentTimeMillis() - lastObservationTimeMs;
    }

    /**
     * Sem observação de pose: o AprilTagProcessor não tem a pose da câmera no
     * robô configurada (setCameraPose), então robotPose não representaria o
     * robô. Devolve null até essa configuração existir.
     */
    @Override
    public PoseObservation getPoseObservation() {
        return null;
    }

    @Override
    public boolean hasTarget() {
        return atualDetection != null && atualDetection.ftcPose != null;
    }

    @Override
    public int getTargetId() {
        if (!hasTarget()) {
            return -1;
        }

        return atualDetection.id;
    }

    @Override
    public double getRangeInches() {
        if (!hasTarget()) {
            return 0.0;
        }

        return atualDetection.ftcPose.range;
    }

    @Override
    public double getBearingDegrees() {
        if (!hasTarget()) {
            return 0.0;
        }

        return atualDetection.ftcPose.bearing;
    }

    @Override
    public double getYawDegrees() {
        if (!hasTarget()) {
            return 0.0;
        }

        return atualDetection.ftcPose.yaw;
    }

    @Override
    public double getForwardInches() {
        if (!hasTarget()) {
            return 0.0;
        }

        return atualDetection.ftcPose.y;
    }

    @Override
    public double getSideInches() {
        if (!hasTarget()) {
            return 0.0;
        }

        return atualDetection.ftcPose.x;
    }

    @Override
    public void close() {
        if (visionPortal != null) {
            visionPortal.close();
            visionPortal = null;
        }

        aprilTagProcessor = null;
        atualDetection = null;
    }
}
