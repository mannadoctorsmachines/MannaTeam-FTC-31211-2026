package org.firstinspires.ftc.teamcode.drive;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.core.RConstants;
import org.firstinspires.ftc.teamcode.util.MathU;
import org.firstinspires.ftc.teamcode.util.PIDController;

public class DriveSistema {

    private DcMotorEx leftMotor;
    private DcMotorEx rightMotor;

    private IMU imu;
    private PIDController turnPID;

    public void init(HardwareMap hardwareMap) {
        leftMotor = hardwareMap.get(DcMotorEx.class, RConstants.LEFT_DRIVE);
        rightMotor = hardwareMap.get(DcMotorEx.class, RConstants.RIGHT_DRIVE);

        leftMotor.setDirection(DcMotor.Direction.REVERSE);
        rightMotor.setDirection(DcMotor.Direction.FORWARD);

        setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        setRunMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        imu = hardwareMap.get(IMU.class, "imu");

        IMU.Parameters parameters = new IMU.Parameters(
                new RevHubOrientationOnRobot(
                        RevHubOrientationOnRobot.LogoFacingDirection.UP,
                        RevHubOrientationOnRobot.UsbFacingDirection.FORWARD
                )
        );

        imu.initialize(parameters);
        resetHeading();

        turnPID = new PIDController(
                RConstants.TURN_KP,
                RConstants.TURN_KI,
                RConstants.TURN_KD
        );
    }


    public void driveArcade(double forward, double turn, double speedMultiplier) {
        forward = MathU.clamp(forward, -1.0, 1.0);
        turn = MathU.clamp(turn, -1.0, 1.0);
        speedMultiplier = MathU.clamp(speedMultiplier, 0.0, 1.0);

        double leftPower = forward + turn;
        double rightPower = forward - turn;

        double max = Math.max(1.0, Math.max(Math.abs(leftPower), Math.abs(rightPower)));

        leftPower = (leftPower / max) * speedMultiplier;
        rightPower = (rightPower / max) * speedMultiplier;

        setTankPowers(leftPower, rightPower);
    }


     // left stick Y  = lado esquerdo
     // right stick Y = lado direito

    public void driveTank(double leftPower, double rightPower, double speedMultiplier) {
        leftPower = MathU.clamp(leftPower, -1.0, 1.0);
        rightPower = MathU.clamp(rightPower, -1.0, 1.0);
        speedMultiplier = MathU.clamp(speedMultiplier, 0.0, 1.0);

        setTankPowers(
                leftPower * speedMultiplier,
                rightPower * speedMultiplier
        );
    }

    public void setTankPowers(double leftPower, double rightPower) {
        leftPower = MathU.clamp(leftPower, -1.0, 1.0);
        rightPower = MathU.clamp(rightPower, -1.0, 1.0);

        leftMotor.setPower(leftPower);
        rightMotor.setPower(rightPower);
    }

    public void stop() {
        setTankPowers(0.0, 0.0);
    }

    public void resetHeading() {
        imu.resetYaw();
    }

    public double getHeadingDegrees() {
        return imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES);
    }

    public double getTurnPowerToHeading(double targetHeadingDegrees) {
        double currentHeading = getHeadingDegrees();
        double error = MathU.normalizarAngulo(targetHeadingDegrees - currentHeading);

        double output = turnPID.calculate(0.0, -error);

        return MathU.clamp(output, -0.45, 0.45);
    }

    public void turnToHeading(double targetHeadingDegrees) {
        double turnPower = getTurnPowerToHeading(targetHeadingDegrees);

        setTankPowers(turnPower, -turnPower);
    }

    public void resetEncoders() {
        setRunMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        setRunMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    public void encoderDriveCm(double cm, double power) {
        int ticks = (int) Math.round(cm * RConstants.TICKS_PER_CM);

        resetEncoders();

        leftMotor.setTargetPosition(ticks);
        rightMotor.setTargetPosition(ticks);

        setRunMode(DcMotor.RunMode.RUN_TO_POSITION);

        power = Math.abs(MathU.clamp(power, 0.0, 1.0));

        setTankPowers(power, power);
    }

    public void encoderTurnCm(double leftCm, double rightCm, double power) {
        int leftTicks = (int) Math.round(leftCm * RConstants.TICKS_PER_CM);
        int rightTicks = (int) Math.round(rightCm * RConstants.TICKS_PER_CM);

        resetEncoders();

        leftMotor.setTargetPosition(leftTicks);
        rightMotor.setTargetPosition(rightTicks);

        setRunMode(DcMotor.RunMode.RUN_TO_POSITION);

        power = Math.abs(MathU.clamp(power, 0.0, 1.0));

        setTankPowers(power, power);
    }

    public boolean isBusy() {
        return leftMotor.isBusy() || rightMotor.isBusy();
    }

    public void setRunMode(DcMotor.RunMode mode) {
        leftMotor.setMode(mode);
        rightMotor.setMode(mode);
    }

    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior) {
        leftMotor.setZeroPowerBehavior(behavior);
        rightMotor.setZeroPowerBehavior(behavior);
    }
}