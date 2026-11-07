package org.firstinspires.ftc.teamcode.Subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
public class Catapult extends SubsystemBase{
    private final DcMotor catapultRight;
    private final DcMotor catapultLeft;


    public Catapult(HardwareMap hardwareMap, String name) {
// Make sure "catapult" matches your config on the Driver Station
        catapultRight = hardwareMap.get(DcMotor.class, "catapultRight");
        catapultLeft = hardwareMap.get(DcMotor.class, "catapultLeft");

        catapultRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        catapultLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        catapultRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        catapultLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    // Set motor power directly (clamped to -1.0 to 1.0)
    public void setPower(double power) {
        catapultRight.setPower(Math.max(-1.0, Math.min(1.0, power)));
        catapultLeft.setPower(Math.max(-1.0, Math.min(1.0, power)));
    }

    public void stop() {
        catapultRight.setPower(0);
        catapultLeft.setPower(0);
    }


    public void periodic() {
// Runs every scheduler loop if registered
    }
}
