package org.firstinspires.ftc.teamcode;

import com.seattlesolvers.solverslib.command.button.GamepadButton;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.DrivetrainController;
import org.firstinspires.ftc.teamcode.subsystems.Catapult;
import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.subsystems.Intake;

import java.util.function.BooleanSupplier;

public class TeleOpBindings {
    private final GamepadEx driver1;
    private final GamepadEx driver2;
    private final Intake intake;
    private final Catapult catapult;
    private final Drivetrain drivetrain;

    // intake button declarations
    private final GamepadButton intakeIn;
    private final GamepadButton intakeOut;
    private final GamepadButton intakeStop; // Triangle on PS5

    public TeleOpBindings(GamepadEx driver1, GamepadEx driver2, Intake intake, Catapult catapult, Drivetrain drivetrain) {
        this.driver1 = driver1;
        this.driver2 = driver2;
        this.intake = intake;
        this.catapult = catapult;
        this.drivetrain = drivetrain;

        // intake
        intakeIn = new GamepadButton(driver1, GamepadKeys.Button.A);
        intakeOut = new GamepadButton(driver1, GamepadKeys.Button.B);
        intakeStop = new GamepadButton(driver1, GamepadKeys.Button.Y); // Y = Triangle on PS5
    }

    public void controlMap(BooleanSupplier started) {
        // intake
        intakeIn.whenActive(() -> intake.intake(0.7));
        intakeOut.whenActive(() -> intake.intake(-0.7));
        intakeStop.whenActive(() -> intake.intake(0.0)); // Triangle = stop intake

        // The subsystem supervises the entire fire/return cycle.
        catapult.bindControls(
                () -> driver1.getTrigger(GamepadKeys.Trigger.RIGHT_TRIGGER) > 0.5,
                () -> driver1.getButton(GamepadKeys.Button.RIGHT_BUMPER),
                () -> driver1.getButton(GamepadKeys.Button.BACK) || driver2.getButton(GamepadKeys.Button.BACK),
                () -> driver2.getButton(GamepadKeys.Button.X) && driver2.getButton(GamepadKeys.Button.Y),
                started);
    }

    public void configureDefaultCommands() {
        drivetrain.setDefaultCommand(new DrivetrainController(
                drivetrain,
                () -> -driver1.getLeftY(),
                driver1::getRightX,
                driver1::getLeftX
        ));
    }
}
