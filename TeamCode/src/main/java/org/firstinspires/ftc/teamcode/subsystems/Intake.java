package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.motors.Motor;

public class Intake extends SubsystemBase {
    private final Motor intakeMotor;

    public Intake(final HardwareMap hMap) {
        intakeMotor = new Motor(hMap, "intakeMotor", Motor.GoBILDA.RPM_435);

        intakeMotor.setRunMode(Motor.RunMode.RawPower);
        intakeMotor.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE); // stops quickly when power is 0
    }

    public void intake(double power) {
        intakeMotor.set(power);
    }
}