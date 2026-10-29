package org.firstinspires.ftc.teamcode.Commands;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.Subsystems.Drivetrain;

import java.util.function.DoubleSupplier;

public class DrivetrainController extends CommandBase {

    private final Drivetrain drivetrain;
    private final DoubleSupplier d_forward;
    private final DoubleSupplier d_rotation;
    private final DoubleSupplier d_strafe;

    public DrivetrainController(
            Drivetrain subsystem,
            DoubleSupplier forward,
            DoubleSupplier rotation,
            DoubleSupplier strafe) {

        drivetrain = subsystem;
        d_forward = forward;
        d_rotation = rotation;
        d_strafe = strafe;

        addRequirements(drivetrain);
    }

    @Override
    public void execute() {
        drivetrain.drive(
                d_strafe.getAsDouble(),
                d_forward.getAsDouble(),
                d_rotation.getAsDouble()
        );
    }
}