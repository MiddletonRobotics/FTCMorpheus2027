package org.firstinspires.ftc.teamcode.opModes.teleOp;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;

import org.firstinspires.ftc.teamcode.subsystems.Catapult;
import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.TeleOpBindings;
@TeleOp(name = "TeleOpMorph - Robot Centric")

public class MainTeleOp extends CommandOpMode {
    @Override
    public void initialize() {
        // Initialize robot, which contains subsystems
        Drivetrain drivetrain = new Drivetrain(hardwareMap);
        Intake intake = new Intake(hardwareMap);
        Catapult catapult = new Catapult(hardwareMap);

        GamepadEx driver1 = new GamepadEx(gamepad1);
        GamepadEx driver2 = new GamepadEx(gamepad2);

        // Initalize controller bindings
        TeleOpBindings bindings = new TeleOpBindings(driver1, driver2, intake, catapult, drivetrain);

        // Actually set the bindings to the control map, and config the default commands
        bindings.controlMap();
        bindings.configureDefaultCommands();
    }
}