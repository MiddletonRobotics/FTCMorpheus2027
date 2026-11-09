package org.firstinspires.ftc.teamcode;

import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;
import com.seattlesolvers.solverslib.command.button.GamepadButton;
import com.seattlesolvers.solverslib.command.button.Trigger;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.DrivetrainController;
import org.firstinspires.ftc.teamcode.subsystems.Catapult;
import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.subsystems.Intake;

public class TeleOpBindings {
    private final GamepadEx driver1;
    private final Intake intake;
    private final Catapult catapult;
    private final Drivetrain drivetrain;

    // intake button declarations
    private final GamepadButton intakeIn;
    private final GamepadButton intakeOut;
    private final GamepadButton intakeStop; // Triangle on PS5

    // catapult button/trigger declarations
    private final Trigger catapultLaunch; // R2 trigger (> 0.5)
    private final GamepadButton catapultReset; // R1 bumper

    public TeleOpBindings(GamepadEx driver1, GamepadEx driver2, Intake intake, Catapult catapult, Drivetrain drivetrain) {
        this.driver1 = driver1;
        this.intake = intake;
        this.catapult = catapult;
        this.drivetrain = drivetrain;

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
        intakeIn.whenActive(() -> intake.intake(0.7));
        intakeOut.whenActive(() -> intake.intake(-0.7));
        intakeStop.whenActive(() -> intake.intake(0.0)); // Triangle = stop intake

        // catapult: R2 = launch, R1 = lower (one press each)
        catapultLaunch.whenActive(new SequentialCommandGroup(
                new InstantCommand(() -> catapult.setPower(1.0), catapult),
                new WaitCommand(500), // TUNE: launch time in ms
                new InstantCommand(() -> catapult.setPower(0.0), catapult)
        ));

        catapultReset.whenActive(new SequentialCommandGroup(
                new InstantCommand(() -> catapult.setPower(-1), catapult),
                new WaitCommand(800), // TUNE: lower time in ms
                new InstantCommand(() -> catapult.setPower(0.0), catapult)
        ));
    }

    public void configureDefaultCommands() {
        drivetrain.setDefaultCommand(new DrivetrainController(
                drivetrain,
                () -> -driver1.getLeftY(),
                () -> driver1.getRightX(),
                () -> driver1.getLeftX()
        ));
    }
}