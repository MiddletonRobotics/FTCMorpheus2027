package org.firstinspires.ftc.teamcode.subsystems;

import com.pedropathing.follower.Follower;
import com.pedropathing.follower.ManualDrive;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

public class Drivetrain extends SubsystemBase {
    private final Follower follower;

    public Drivetrain(final HardwareMap hMap) {
        follower = Constants.createFollower(hMap);
    }

    public void drive(double strafe, double forward, double rotation) {
        follower.manual(ManualDrive.fieldCentric(
                forward,
                strafe,
                rotation,
                follower.pose().heading()
        ));
    }

    @Override
    public void periodic() {
        follower.update();
    }


    public Follower getFollower() {
        return follower;
    }
}