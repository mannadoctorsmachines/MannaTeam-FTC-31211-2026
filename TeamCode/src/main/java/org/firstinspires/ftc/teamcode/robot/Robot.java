package org.firstinspires.ftc.teamcode.robot;

import org.firstinspires.ftc.teamcode.subsystems.shooter.Shooter;

public class Robot {

    private final Shooter shooter;

    public Robot() {
        shooter = new Shooter();
    }

    public Shooter getShooter() {
        return shooter;
    }
}