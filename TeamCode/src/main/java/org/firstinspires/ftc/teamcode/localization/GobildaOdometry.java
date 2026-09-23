package org.firstinspires.ftc.teamcode.localization;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public class GobildaOdometry {
    GoBildaPinpointDriver odo;
    double initTime = 0;

    public void init(HardwareMap hardwareMap){

        odo = hardwareMap.get(GoBildaPinpointDriver.class, "odometry");

        odo.setOffsets(-18, -10, DistanceUnit.CM);
        odo.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
        odo.setEncoderDirections(GoBildaPinpointDriver.EncoderDirection.FORWARD,
                                 GoBildaPinpointDriver.EncoderDirection.FORWARD);


        odo.resetPosAndIMU();

    }

}
