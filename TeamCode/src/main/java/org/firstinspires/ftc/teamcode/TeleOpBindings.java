package org.firstinspires.ftc.teamcode;

import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.Subsystem;
import com.seattlesolvers.solverslib.command.WaitCommand;
import com.seattlesolvers.solverslib.command.button.GamepadButton;
import com.seattlesolvers.solverslib.command.button.Trigger;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.Commands.DrivetrainController;
import org.firstinspires.ftc.teamcode.Subsystems.Morpheus;

public class TeleOpBindings {
    private final GamepadEx driver1;
    private final Morpheus robot;

    // intake button declarations
    private final GamepadButton intakeIn;
    private final GamepadButton intakeOut;
    private final GamepadButton intakeStop; // Triangle on PS5

    // catapult button/trigger declarations
    private final Trigger catapultLaunch; // R2 trigger (> 0.5)
    private final GamepadButton catapultReset; // R1 bumper

    public TeleOpBindings(GamepadEx driver1, Morpheus robot) {
        this.driver1 = driver1;
        this.robot = robot;

// intake
        intakeIn = new GamepadButton(driver1, GamepadKeys.Button.A);
        intakeOut = new GamepadButton(driver1, GamepadKeys.Button.B);
        intakeStop = new GamepadButton(driver1, GamepadKeys.Button.Y); // Y = Triangle on PS5

// catapult controls setup
        catapultLaunch = new Trigger(() -> driver1.getTrigger(GamepadKeys.Trigger.RIGHT_TRIGGER) > 0.5);
        catapultReset = new GamepadButton(driver1, GamepadKeys.Button.RIGHT_BUMPER);
    }

    public void controlMap() {
// intake
        intakeIn.whenActive(() -> robot.intake.intake(0.7));
        intakeOut.whenActive(() -> robot.intake.intake(-0.7));
        intakeStop.whenActive(() -> robot.intake.intake(0.0)); // Triangle = stop intake

// catapult: R2 = launch, R1 = lower (one press each)
        catapultLaunch.whenActive(new SequentialCommandGroup(
                new InstantCommand(() -> robot.catapult.setPower(1.0), robot.catapult),
                new WaitCommand(500), // TUNE: launch time in ms
                new InstantCommand(() -> robot.catapult.setPower(0.0), robot.catapult)
        ));

        catapultReset.whenActive(new SequentialCommandGroup(
                new InstantCommand(() -> robot.catapult.setPower(-1), robot.catapult),
                new WaitCommand(800), // TUNE: lower time in ms
                new InstantCommand(() -> robot.catapult.setPower(0.0), robot.catapult)
        ));
    }

    public void configureDefaultCommands() {
        robot.drivetrain.setDefaultCommand(new DrivetrainController(
                robot.drivetrain,
                () -> driver1.getLeftY(),
                () -> -driver1.getRightX(),
                () -> -driver1.getLeftX()
        ));
    }
}