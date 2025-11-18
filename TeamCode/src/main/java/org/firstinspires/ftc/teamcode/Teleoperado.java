/* Copyright (c) 2017 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.teamcode;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;


/*
 * This file contains an minimal example of a Linear "OpMode". An OpMode is a 'program' that runs in either
 * the autonomous or the teleop period of an FTC match. The names of OpModes appear on the menu
 * of the FTC Driver Station. When a selection is made from the menu, the corresponding OpMode
 * class is instantiated on the Robot Controller and executed.
 *
 * This particular OpMode just executes a basic Tank Drive Teleop for a two wheeled robot
 * It includes all the skeletal structure that all linear OpModes contain.
 *
 * Use Android Studio to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this OpMode to the Driver Station OpMode list
 */

@TeleOp(name="teleoparado", group="Linear OpMode")
public class Teleoperado extends LinearOpMode {

    // Declare OpMode members.
    private ElapsedTime runtime = new ElapsedTime();
    private CRServo conveorServo = null;
    private DcMotorEx esteira = null;
    private DcMotor intake  = null;
    private CRServo ServoInt = null;
    private DcMotor leftDrive = null;
    private DcMotor rightDrive = null;
    private AnalogInput sensor = null;
    public static double esteiraRPM = 0;



    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initialized");
        telemetry.update();

        // Initialize the hardware variables. Note that the strings used here as parameters
        // to 'get' must correspond to the names assigned during the robot configuration
        // step (using the FTC Robot Controller app on the phone).

        leftDrive = hardwareMap.get(DcMotor.class, "esquerda");
        rightDrive = hardwareMap.get(DcMotor.class, "direita");
        //CRServo conveyorServo = hardwareMap.get(CRServo.class, "bolinha_servo");
        esteira = hardwareMap.get(DcMotorEx.class, "esteira");
        intake = hardwareMap.get(DcMotor.class, "intake");
        ServoInt = hardwareMap.get(CRServo.class, "ServoInt");
        sensor = hardwareMap.get(AnalogInput.class, "sensor");

        leftDrive.setDirection(DcMotor.Direction.REVERSE);
        rightDrive.setDirection(DcMotor.Direction.FORWARD);
        // To drive forward, most robots need the motor on one side to be reversed, because the axles point in opposite directions.
        // Pushing the left stick forward MUST make robot go forward. So adjust these two lines based on your first test drive.
        // Note: The settings here assume direct drive on left and right wheels.  Gear Reduction or 90 Deg drives may require direction flips


        // inverter o sentido de giro:
        // conveyorServo.setDirection(Servo.Direction.REVERSE);

        // Wait for the game to start (driver presses START)
        waitForStart();
        runtime.reset();

        // run until the end of the match (driver presses STOP)
        while (opModeIsActive()) {

            // Setup a variable for each drive wheel to save power level for telemetry
            double leftPower;
            double rightPower;

            double drive = -gamepad1.left_stick_y;
            double turn = gamepad1.right_stick_x;
            leftPower = Range.clip(drive + turn, -1.0, 1.0);
            rightPower = Range.clip(drive - turn, -1.0, 1.0);
            telemetry.addData("Modo", "DIREÇÃO MANUAL");

            if (gamepad1.y) {

                double erro = sensor.getVoltage() - 2;
                double erroFinal = erro * - 0.5;

                leftDrive.setPower(erroFinal);
                rightDrive.setPower(erroFinal);

            } else {

                leftDrive.setPower(leftPower);
                rightDrive.setPower(rightPower);
            }

                // Send calculated power to wheels

                //Coletor
                double intakePower = 0;
                double ServoIntPower = 0;

                if (gamepad2.y) {
                    intakePower = 1.0;
                    ServoIntPower = 1;
                } else if (gamepad2.b) {
                    intakePower = -1.0;
                    ServoIntPower = -1;

                } else {
                    intakePower = 0.0;
                    ServoIntPower = 0;
                }



               /*   //shooter

               if(gamepad2.a){
                    esteira.setPower(1);
                }
                else{esteira.setPower(0);}
               */


                //Esteira
                double EsteiraPower = -gamepad2.right_trigger;

                if(gamepad2.right_trigger > 0.2){
                    esteira.setPower((esteiraRPM - 4100)* 0.05);
                }
                else{
                    esteira.setPower(0);
                }

                intake.setPower(intakePower);
                ServoInt.setPower(ServoIntPower);

                double ticksPorSegundo = esteira.getVelocity();
                double ticksPorRevolucao = 28.0; // a maioria dos motores REV tem 28 ticks por revolução do eixo do motor

                esteiraRPM = Math.abs((ticksPorSegundo / ticksPorRevolucao) * 60.0);

                // Show the elapsed game time and wheel power.
                telemetry.addData("Status", "Run Time: " + runtime.toString());
                telemetry.addData("Motors", "left (%.2f), right (%.2f)", leftPower, rightPower);
                telemetry.addData("coletor", "power (%.2f)", intakePower);
                telemetry.addData("Esteira", "power (%.2f)", EsteiraPower);
                telemetry.addData("Esteira Power", EsteiraPower);
                telemetry.addData("Esteira RPM", esteiraRPM);
                telemetry.update();
            }
        }


    }
















































































































































































































































































































































































































































































































































































































































































































































































































































    // feito pelos cupinchas, rsrsrsrsrrsrs!