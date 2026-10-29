package org.firstinspires.ftc.teamcode;

import com.seattlesolvers.solverslib.command.button.GamepadButton;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.Commands.DrivetrainController;

import org.firstinspires.ftc.teamcode.Subsystems.Morpheus;

public class TeleOpBindings {
    private final GamepadEx driver1;
    private final Morpheus robot;

    // intake button declerations
    private final GamepadButton intakeIn;
    private final GamepadButton intakeOut;

    public TeleOpBindings(GamepadEx driver1, Morpheus robot) {
        this.driver1 = driver1;
        this.robot = robot;

        // intake
        // button a is bottom, button b is left on the PS controller
        intakeIn = new GamepadButton(driver1, GamepadKeys.Button.A);
        intakeOut = new GamepadButton(driver1, GamepadKeys.Button.B);
    }

    public void controlMap() {
        // intake
        intakeIn.whenActive(() -> robot.intake.intake(1));
        intakeOut.whenActive(() -> robot.intake.intake(-1));
    }

    public void configureDefaultCommands() {
        robot.drivetrain.setDefaultCommand(new DrivetrainController(
                robot.drivetrain,
                () -> driver1.getLeftY(),
                () -> driver1.getRightX(),
                () -> driver1.getLeftX()
        ));
    }
}