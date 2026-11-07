package org.firstinspires.ftc.teamcode.OpModes.TeleOp;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;

import org.firstinspires.ftc.teamcode.Subsystems.Morpheus;
import org.firstinspires.ftc.teamcode.TeleOpBindings;
@TeleOp

@TeleOp(name = "TeleOpMorph")
public class MainTeleOp extends CommandOpMode {
    @Override
    public void initialize() {
        // Initialize robot, which contains subsystems
        Morpheus robot = new Morpheus(hardwareMap, telemetry);

        GamepadEx driver1 = new GamepadEx(gamepad1);
        GamepadEx driver2 = new GamepadEx(gamepad2);

        // Initalize controller bindings
        TeleOpBindings bindings = new TeleOpBindings(driver1, driver2, robot);

        // Actually set the bindings to the control map, and config the default commands
        bindings.controlMap();
        bindings.configureDefaultCommands();
    }
}

