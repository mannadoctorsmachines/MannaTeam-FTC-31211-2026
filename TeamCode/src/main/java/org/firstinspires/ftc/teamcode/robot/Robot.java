package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.subsystems.shooter.Shooter;

public class Robot {

    private final Shooter shooter;

    public Robot() {
        shooter = new Shooter();
    }

    public void init(HardwareMap hardwareMap) {
        shooter.init(hardwareMap);
    }

    public Shooter getShooter() {
        return shooter;
    }
}