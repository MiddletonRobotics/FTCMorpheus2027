package org.firstinspires.ftc.teamcode.opModes.teleOp;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;

import org.firstinspires.ftc.teamcode.subsystems.Catapult;
import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.TeleOpBindings;

@TeleOp (name = "BlueTeleOp - Field Centric")
public class BlueTeleOp extends CommandOpMode {
    @Override
    public void initialize() {
        Drivetrain drivetrain = new Drivetrain(hardwareMap);
        Intake intake = new Intake(hardwareMap);
        Catapult catapult = new Catapult(hardwareMap);

        // set starting heading for blue alliance (0 degrees)
        drivetrain.getFollower().setPose(new Pose(0, 0, Math.toRadians(0)));

        GamepadEx driver1 = new GamepadEx(gamepad1);
        GamepadEx driver2 = new GamepadEx(gamepad2);

        // init controller bindings
        TeleOpBindings bindings = new TeleOpBindings(driver1, driver2, intake, catapult, drivetrain);

        // actually set the bindings to the control map, and config the default commands
        bindings.controlMap();
        bindings.configureDefaultCommands();
    }

    @Override
    public void run() {
        super.run();
        telemetry.update();
    }
}