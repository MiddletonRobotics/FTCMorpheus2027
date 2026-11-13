package org.firstinspires.ftc.teamcode.subsystems;
import com.seattlesolvers.solverslib.command.SubsystemBase;
/** Records arguments from the REAL DrivetrainController; no physical motor model. */
public class Drivetrain extends SubsystemBase {
    public double strafe, forward, rotation;
    public void drive(double strafe, double forward, double rotation) {
        this.strafe = strafe; this.forward = forward; this.rotation = rotation;
    }
}
