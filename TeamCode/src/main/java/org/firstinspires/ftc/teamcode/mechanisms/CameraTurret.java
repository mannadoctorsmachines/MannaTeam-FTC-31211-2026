// ==========================================================================
// ARQUIVADO em 2026-07-22: sistema de torreta (CameraTurret) removido do robô.
// O robô passou a usar shooter fixo; a mira agora é feita girando o chassi
// (ver DriveSistema2.turnToHeading + AutonomoLongo.aimByTurningChassis).
//
// Este arquivo foi mantido comentado apenas como referência histórica, caso
// o time volte a montar uma versão do robô com torreta no futuro. Também
// existe (ou deve existir) uma branch/tag no GitHub com a versão completa
// da torreta funcionando, para não depender só deste comentário.
//
// Constantes de PID da torreta (TURRET_KP/KI/KD, TURRET_DEADBAND_DEGREES,
// TURRET_MAX_POWER, TURRET_MIN_POWER, TURRET_SEARCH_POWER) e o nome de
// hardware CAMERA_TURRET foram removidos do RConstants junto com este
// arquivo.
// ==========================================================================
//package org.firstinspires.ftc.teamcode.mechanisms;

//import com.qualcomm.robotcore.hardware.DcMotor;
//import com.qualcomm.robotcore.hardware.DcMotorEx;
//import com.qualcomm.robotcore.hardware.HardwareMap;

//import org.firstinspires.ftc.teamcode.core.RConstants;
//import org.firstinspires.ftc.teamcode.util.MathU;
//import org.firstinspires.ftc.teamcode.util.PIDController;

//public class CameraTurret {

//    private DcMotorEx turretMotor;
//    private PIDController pid;

//    private boolean trackingEnabled = true;

//    private double smoothedBearing = 0.0;
//    private boolean firstSample = true;

//    private int lastSeenDirection = 1;

//    public void init(HardwareMap hardwareMap) {
//        turretMotor = hardwareMap.get(DcMotorEx.class, RConstants.CAMERA_TURRET);

//        turretMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
//        turretMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

//        pid = new PIDController(
//                RConstants.TURRET_KP,
//                RConstants.TURRET_KI,
//                RConstants.TURRET_KD
//        );
//    }

//    public void updateTracking(boolean hasTarget, double bearingDegrees) {
//        if (!trackingEnabled) {
//            stop();
//            return;
//        }

//        if (hasTarget) {
//            trackTarget(bearingDegrees);
//        } else {
//            searchTarget();
//        }
//    }

//    private void trackTarget(double bearingDegrees) {
//        updateSmoothedBearing(bearingDegrees);

//        if (Math.abs(smoothedBearing) <= RConstants.TURRET_DEADBAND_DEGREES) {
//            stop();
//            return;
//        }

//        updateLastSeenDirection();

//        double power = pid.calculate(0.0, smoothedBearing); //<---- caso a torreta gire para o lado errado, ajustar o smoothedBearing para negativo(-smoothedBearing)

//        power = MathU.clamp(
//                power,
//                -RConstants.TURRET_MAX_POWER,
//                RConstants.TURRET_MAX_POWER
//        );

//        if (Math.abs(power) < RConstants.TURRET_MIN_POWER) {
//            power = Math.signum(power) * RConstants.TURRET_MIN_POWER;
//        }

//        turretMotor.setPower(power);
//    }

//    private void updateSmoothedBearing(double bearingDegrees) {
//        if (firstSample) {
//            smoothedBearing = bearingDegrees;
//            firstSample = false;
//            return;
//        }

//        double smoothingAlpha = 0.25;

//        smoothedBearing =
//                (smoothingAlpha * bearingDegrees)
//                        + ((1.0 - smoothingAlpha) * smoothedBearing);
//    }

//    private void updateLastSeenDirection() {
//        if (smoothedBearing > 0.0) {
//            lastSeenDirection = 1;
//        } else {
//            lastSeenDirection = -1;
//        }
//    }

//    private void searchTarget() {
//        firstSample = true;
//        pid.reset();

//        turretMotor.setPower(RConstants.TURRET_SEARCH_POWER * lastSeenDirection);
//    }

//    public void enableTracking() {
//        trackingEnabled = true;
//    }

//    public void disableTracking() {
//        trackingEnabled = false;
//        stop();
//    }

//    public boolean isTrackingEnabled() {
//        return trackingEnabled;
//    }

//    public void stop() {
//        if (turretMotor != null) {
//            turretMotor.setPower(0.0);
//        }
//    }
//}