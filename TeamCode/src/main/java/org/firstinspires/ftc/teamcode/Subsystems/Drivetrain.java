package org.firstinspires.ftc.teamcode.Subsystems;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

public class Drivetrain extends SubsystemBase {
    private final Follower follower;

    public Drivetrain(final HardwareMap hMap, final String name) {
        follower = Constants.createFollower(hMap);
    }

    public void drive(double strafe, double forward, double rotation) {
        follower.manual(forward, strafe, rotation);
    }

    @Override
    public void periodic() {
        follower.update();
    }

    public Follower getFollower() {
        return follower;
    }
}