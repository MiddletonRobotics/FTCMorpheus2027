package org.firstinspires.ftc.teamcode.OpModes.TeleOp;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;

import org.firstinspires.ftc.teamcode.Subsystems.Morpheus;
import org.firstinspires.ftc.teamcode.Subsystems.Morpheus;
import org.firstinspires.ftc.teamcode.TeleOpBindings;

@TeleOp (name = "RedTeleOp")
public class RedTeleOp extends CommandOpMode {
    @Override
    public void initialize() {
        // Initialize robot, which contains subsystems
        Morpheus robot = new Morpheus(hardwareMap, telemetry);

        // set starting heading for red alliance (180 degrees)
        robot.drivetrain.getFollower().setPose(new Pose(0, 0, Math.toRadians(180)));

        GamepadEx driver1 = new GamepadEx(gamepad1);
        GamepadEx driver2 = new GamepadEx(gamepad2);

        // init controller bindings
        TeleOpBindings bindings = new TeleOpBindings(driver1, robot);

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
