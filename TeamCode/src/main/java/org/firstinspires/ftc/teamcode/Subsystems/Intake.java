package org.firstinspires.ftc.teamcode.Subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.motors.Motor;

public class Intake extends SubsystemBase {
    private final Motor intakeMotor;

    public Intake(final HardwareMap hMap, final String name) {
        intakeMotor = new Motor(hMap, "intakeMotor", Motor.GoBILDA.RPM_435);

        intakeMotor.setRunMode(Motor.RunMode.RawPower);
    }

    public void intake(double power) {
        intakeMotor.set(power);
    }
}