package org.firstinspires.ftc.teamcode.drive;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.core.RConstants;
import org.firstinspires.ftc.teamcode.util.MathU;
import org.firstinspires.ftc.teamcode.util.PIDController;

public class DriveSistema2 {

    private DcMotorEx leftFrontMotor;
    private DcMotorEx rightFrontMotor;
    private DcMotorEx leftBackMotor;
    private DcMotorEx rightBackMotor;

    private IMU imu;
    private PIDController turnPID;

    public void init(HardwareMap hardwareMap) {
        leftFrontMotor = hardwareMap.get(DcMotorEx.class, RConstants.LEFT_DRIVE);
        rightFrontMotor = hardwareMap.get(DcMotorEx.class, RConstants.RIGHT_DRIVE);
        leftBackMotor = hardwareMap.get(DcMotorEx.class, RConstants.LEFTB_DRIVE);
        rightBackMotor = hardwareMap.get(DcMotorEx.class, RConstants.RIGHTB_DRIVE);

        configureMotors();
        configureImu(hardwareMap);

        turnPID = new PIDController(
                RConstants.TURN_KP,
                RConstants.TURN_KI,
                RConstants.TURN_KD
        );
    }

    private void configureMotors() {
        leftFrontMotor.setDirection(DcMotorSimple.Direction.REVERSE);
        leftBackMotor.setDirection(DcMotorSimple.Direction.FORWARD);

        rightFrontMotor.setDirection(DcMotorSimple.Direction.REVERSE);
        rightBackMotor.setDirection(DcMotorSimple.Direction.REVERSE);

        setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        setRunMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    private void configureImu(HardwareMap hardwareMap) {
        imu = hardwareMap.get(IMU.class, RConstants.IMU);

        IMU.Parameters parameters = new IMU.Parameters(
                new RevHubOrientationOnRobot(
                        RevHubOrientationOnRobot.LogoFacingDirection.UP,
                        RevHubOrientationOnRobot.UsbFacingDirection.FORWARD
                )
        );

        imu.initialize(parameters);
        resetHeading();
    }

    public void driveMecanum(double axial, double lateral, double yaw, double speedMultiplier) {
        axial = MathU.clamp(axial, -1.0, 1.0);
        lateral = MathU.clamp(lateral, -1.0, 1.0);
        yaw = MathU.clamp(yaw, -1.0, 1.0);
        speedMultiplier = MathU.clamp(speedMultiplier, 0.0, 1.0);

        double leftFrontPower = axial + lateral + yaw;
        double rightFrontPower = axial - lateral - yaw;
        double leftBackPower = axial - lateral + yaw;
        double rightBackPower = axial + lateral - yaw;

        double max = Math.max(
                Math.max(Math.abs(leftFrontPower), Math.abs(rightFrontPower)),
                Math.max(Math.abs(leftBackPower), Math.abs(rightBackPower))
        );

        if (max < 1.0) {
            max = 1.0;
        }

        leftFrontPower = leftFrontPower / max * speedMultiplier;
        rightFrontPower = rightFrontPower / max * speedMultiplier;
        leftBackPower = leftBackPower / max * speedMultiplier;
        rightBackPower = rightBackPower / max * speedMultiplier;

        setMecanumPowers(
                leftFrontPower,
                rightFrontPower,
                leftBackPower,
                rightBackPower
        );
    }

    public void setMecanumPowers(
            double leftFrontPower,
            double rightFrontPower,
            double leftBackPower,
            double rightBackPower
    ) {
        leftFrontPower = MathU.clamp(leftFrontPower, -1.0, 1.0);
        rightFrontPower = MathU.clamp(rightFrontPower, -1.0, 1.0);
        leftBackPower = MathU.clamp(leftBackPower, -1.0, 1.0);
        rightBackPower = MathU.clamp(rightBackPower, -1.0, 1.0);

        leftFrontMotor.setPower(leftFrontPower);
        rightFrontMotor.setPower(rightFrontPower);
        leftBackMotor.setPower(leftBackPower);
        rightBackMotor.setPower(rightBackPower);
    }

    public void stop() {
        setMecanumPowers(0, 0, 0, 0);
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

        if (Math.abs(error) <= RConstants.TURN_TOLERANCE_DEGREES) {
            return 0.0;
        }

        double output = turnPID.calculate(0.0, -error);

        output = MathU.clamp(
                output,
                -RConstants.TURN_MAX_POWER,
                RConstants.TURN_MAX_POWER
        );

        if (Math.abs(output) < RConstants.TURN_MIN_POWER) {
            output = Math.copySign(RConstants.TURN_MIN_POWER, output);
        }

        return output;
    }

    public void resetTurnController() {
        turnPID.reset();
    }

    public void turnToHeading(double targetHeadingDegrees) {
        double turnPower = getTurnPowerToHeading(targetHeadingDegrees);

        setMecanumPowers(
                turnPower,
                -turnPower,
                turnPower,
                -turnPower
        );
    }

    public void resetEncoders() {
        setRunMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        setRunMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    public void encoderDriveCm(double cm, double power) {
        int ticks = (int) Math.round(cm * RConstants.TICKS_PER_CM);

        resetEncoders();

        leftFrontMotor.setTargetPosition(ticks);
        rightFrontMotor.setTargetPosition(ticks);
        leftBackMotor.setTargetPosition(ticks);
        rightBackMotor.setTargetPosition(ticks);

        setRunMode(DcMotor.RunMode.RUN_TO_POSITION);

        power = Math.abs(MathU.clamp(power, 0.0, 1.0));

        setMecanumPowers(power, power, power, power);
    }

    public void encoderTurnCm(double leftCm, double rightCm, double power) {
        int leftTicks = (int) Math.round(leftCm * RConstants.TICKS_PER_CM);
        int rightTicks = (int) Math.round(rightCm * RConstants.TICKS_PER_CM);

        resetEncoders();

        leftFrontMotor.setTargetPosition(leftTicks);
        leftBackMotor.setTargetPosition(leftTicks);

        rightFrontMotor.setTargetPosition(rightTicks);
        rightBackMotor.setTargetPosition(rightTicks);

        setRunMode(DcMotor.RunMode.RUN_TO_POSITION);

        power = Math.abs(MathU.clamp(power, 0.0, 1.0));

        setMecanumPowers(power, power, power, power);
    }

    /**
     * Deslocamento lateral por encoder para rodas mecanum.
     * Valor positivo desloca para a direita; negativo, para a esquerda.
     */
    public void encoderStrafeCm(double cm, double power) {
        int ticks = (int) Math.round(cm * RConstants.STRAFE_TICKS_PER_CM);

        resetEncoders();

        leftFrontMotor.setTargetPosition(ticks);
        rightFrontMotor.setTargetPosition(-ticks);
        leftBackMotor.setTargetPosition(-ticks);
        rightBackMotor.setTargetPosition(ticks);

        setRunMode(DcMotor.RunMode.RUN_TO_POSITION);

        power = Math.abs(MathU.clamp(power, 0.0, 1.0));
        setMecanumPowers(power, power, power, power);
    }

    public boolean isBusy() {
        return leftFrontMotor.isBusy()
                || rightFrontMotor.isBusy()
                || leftBackMotor.isBusy()
                || rightBackMotor.isBusy();
    }

    /**
     * Distância média realmente registrada pelos quatro encoders desde
     * o último reset. O sinal acompanha o comando enviado ao drive.
     */
    public double getAverageEncoderDistanceCm() {
        double averageTicks = (
                leftFrontMotor.getCurrentPosition()
                        + rightFrontMotor.getCurrentPosition()
                        + leftBackMotor.getCurrentPosition()
                        + rightBackMotor.getCurrentPosition()
        ) / 4.0;

        return averageTicks / RConstants.TICKS_PER_CM;
    }

    public double getAverageStrafeDistanceCm() {
        double averageStrafeTicks = (
                leftFrontMotor.getCurrentPosition()
                        - rightFrontMotor.getCurrentPosition()
                        - leftBackMotor.getCurrentPosition()
                        + rightBackMotor.getCurrentPosition()
        ) / 4.0;

        return averageStrafeTicks / RConstants.STRAFE_TICKS_PER_CM;
    }

    public void setRunMode(DcMotor.RunMode mode) {
        leftFrontMotor.setMode(mode);
        rightFrontMotor.setMode(mode);
        leftBackMotor.setMode(mode);
        rightBackMotor.setMode(mode);
    }

    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior) {
        leftFrontMotor.setZeroPowerBehavior(behavior);
        rightFrontMotor.setZeroPowerBehavior(behavior);
        leftBackMotor.setZeroPowerBehavior(behavior);
        rightBackMotor.setZeroPowerBehavior(behavior);
    }
}
