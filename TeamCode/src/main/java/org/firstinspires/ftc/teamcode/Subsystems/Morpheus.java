package org.firstinspires.ftc.teamcode.Subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class Morpheus {
    public final Drivetrain drivetrain;
    public final Intake intake;

    public final Catapult catapult;

    public Morpheus(HardwareMap hardwareMap, Telemetry telemetry) {
        drivetrain = new Drivetrain(hardwareMap, "Drivetrain");
        intake = new Intake(hardwareMap, "Intake");
        catapult = new Catapult(hardwareMap, "Catapult");
    }
}