package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;

public class Catapult extends SubsystemBase {
    private final DcMotor leftMotor;
    private final DcMotor rightMotor;

    // TUNE: read encoder values from telemetry with the arm at each position
    public static final int HOME_POSITION = 0;
    public static final int FIRE_POSITION = 300;
    public static final double LAUNCH_POWER = 1.0;
    public static final double RETURN_POWER = 0.5;
    private static final int TOLERANCE = 15;

    public Catapult(HardwareMap hardwareMap) {
        // Change these to match your Driver Station config names
        leftMotor = hardwareMap.get(DcMotor.class, "catapultLeft");
        rightMotor = hardwareMap.get(DcMotor.class, "catapultRight");

        // The motors face opposite ways, so one must be reversed.
        // If the arm fights itself, move this to the left motor instead.
        rightMotor.setDirection(DcMotorSimple.Direction.REVERSE);

        for (DcMotor m : new DcMotor[]{leftMotor, rightMotor}) {
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setTargetPosition(0);
            m.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        }
    }

    // Move both motors to an encoder position at the given power
    public void goTo(int ticks, double power) {
        leftMotor.setTargetPosition(ticks);
        rightMotor.setTargetPosition(ticks);
        leftMotor.setPower(power);
        rightMotor.setPower(power);
    }

    // True once both motors are within tolerance of their target
    public boolean atTarget() {
        return Math.abs(leftMotor.getCurrentPosition() - leftMotor.getTargetPosition()) <= TOLERANCE
                && Math.abs(rightMotor.getCurrentPosition() - rightMotor.getTargetPosition()) <= TOLERANCE;
    }

    // For telemetry/tuning: use this to find your real HOME and FIRE values
    public int getPosition() {
        return leftMotor.getCurrentPosition();
    }

    @Override
    public void periodic() {
        // Runs every scheduler loop if registered
    }
}