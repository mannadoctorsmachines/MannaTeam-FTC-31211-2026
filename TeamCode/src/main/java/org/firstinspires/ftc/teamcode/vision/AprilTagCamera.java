package org.firstinspires.ftc.teamcode.vision;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.core.RConstants;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;

public class AprilTagCamera {

    private AprilTagProcessor aprilTagProcessor;
    private VisionPortal visionPortal;

    private AprilTagDetection atualDetection;

    public void init(HardwareMap hardwareMap) {
        aprilTagProcessor = new AprilTagProcessor.Builder()
                .setDrawAxes(true)
                .setDrawCubeProjection(true)
                .setDrawTagID(true)
                .setDrawTagOutline(true)
                .build();

        visionPortal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, RConstants.WEBCAM))
                .addProcessor(aprilTagProcessor)
                .build();
    }

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

            if (RConstants.TARGET_APRIL_TAG_ID >= 0
                    && detection.id != RConstants.TARGET_APRIL_TAG_ID) {
                continue;
            }

            if (detection.ftcPose.range < RangeProximo) {
                RangeProximo = detection.ftcPose.range;
                DetectionProxima = detection;
            }
        }

        atualDetection = DetectionProxima;
    }

    public boolean hasTarget() {
        return atualDetection != null && atualDetection.ftcPose != null;
    }

    public int getTargetId() {
        if (!hasTarget()) {
            return -1;
        }

        return atualDetection.id;
    }

    public double getRangeInches() {
        if (!hasTarget()) {
            return 0.0;
        }

        return atualDetection.ftcPose.range;
    }

    public double getBearingDegrees() {
        if (!hasTarget()) {
            return 0.0;
        }

        return atualDetection.ftcPose.bearing;
    }

    public double getYawDegrees() {
        if (!hasTarget()) {
            return 0.0;
        }

        return atualDetection.ftcPose.yaw;
    }

    public void close() {
        if (visionPortal != null) {
            visionPortal.close();
            visionPortal = null;
        }

        aprilTagProcessor = null;
        atualDetection = null;
    }
}
