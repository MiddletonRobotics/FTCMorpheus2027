package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import com.seattlesolvers.solverslib.command.SubsystemBase;

public class Catapult extends SubsystemBase {
    private final DcMotor leftMotor;
    private final DcMotor rightMotor;

    // goBILDA Yellow Jacket 60 RPM (5203-2402-0100) specifications.
    // https://www.gobilda.com/content/spec_sheets/5203-2402-0100_spec_sheet.pdf
    private static final double MOTOR_RPM = 60.0;
    private static final double MOTOR_GEAR_RATIO = 99.5;
    private static final double MOTOR_TICKS_PER_REV = 2786.2;

    // TUNE: recheck these encoder positions on the 60 RPM gearbox before firing.
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
            // Clone so configuring the catapult cannot change other motors' metadata.
            MotorConfigurationType motorType = m.getMotorType().clone();
            motorType.setMaxRPM(MOTOR_RPM);
            motorType.setGearing(MOTOR_GEAR_RATIO);
            motorType.setTicksPerRev(MOTOR_TICKS_PER_REV);
            m.setMotorType(motorType);

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