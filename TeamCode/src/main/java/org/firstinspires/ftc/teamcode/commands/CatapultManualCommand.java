package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import org.firstinspires.ftc.teamcode.catapult.CatapultManualControl;
import org.firstinspires.ftc.teamcode.subsystems.Catapult;
import java.util.function.BooleanSupplier;

/** One command owns both catapult motors, preventing competing load/fire sequences. */
public final class CatapultManualCommand extends CommandBase {
    private final Catapult catapult;
    private final BooleanSupplier lower, launch, emergencyStop;
    private final CatapultManualControl control = new CatapultManualControl();

    public CatapultManualCommand(Catapult catapult, BooleanSupplier lower,
                                 BooleanSupplier launch, BooleanSupplier emergencyStop) {
        this.catapult = catapult;
        this.lower = lower;
        this.launch = launch;
        this.emergencyStop = emergencyStop;
        addRequirements(catapult);
    }

    @Override public void initialize() { catapult.stop(); }
    @Override public void execute() {
        try {
            catapult.setPower(control.update(System.nanoTime() / 1e9,
                    lower.getAsBoolean(), launch.getAsBoolean(), emergencyStop.getAsBoolean()));
        } catch (RuntimeException e) {
            control.stop();
            catapult.stop();
            throw e;
        }
    }
    @Override public boolean isFinished() { return false; }
    @Override public void end(boolean interrupted) {
        control.stop();
        catapult.stop();
    }
}
