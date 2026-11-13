package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.teamcode.catapult.CatapultCycle;
import org.firstinspires.ftc.teamcode.catapult.CatapultSettings;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/** Sole owner of catapult motors; motion requires measured settings and explicit home. */
public class Catapult extends SubsystemBase {
    private final DcMotorEx leftMotor, rightMotor;
    private final CatapultCycle cycle;
    private final DoubleSupplier clock;
    private double leftVelocity, rightVelocity;
    private final CatapultCycle.Inputs readings = new CatapultCycle.Inputs();
    private BooleanSupplier fire = () -> false, returnHome = () -> false;
    private BooleanSupplier emergencyStop = () -> false, confirmHome = () -> false, started = () -> false;

    public Catapult(HardwareMap hardwareMap) {
        this(hardwareMap, CatapultSettings.measuredConfiguration(), () -> System.nanoTime() * 1e-9);
    }

    // Package-private injection for desktop adapter checks; production uses measuredConfiguration().
    Catapult(HardwareMap hardwareMap, CatapultCycle.Config config, DoubleSupplier clock) {
        cycle = new CatapultCycle(config);
        this.clock = clock;
        leftMotor = hardwareMap.get(DcMotorEx.class, "catapultLeft");
        rightMotor = hardwareMap.get(DcMotorEx.class, "catapultRight");
        stopMotors();
        leftMotor.setDirection(DcMotorSimple.Direction.FORWARD);
        rightMotor.setDirection(DcMotorSimple.Direction.REVERSE); // From new push; verify unloaded.
        for (DcMotor m : new DcMotor[]{leftMotor, rightMotor}) {
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
        // Initialization never resets encoder positions.
    }
    public void bindControls(BooleanSupplier fire, BooleanSupplier returnHome,
                             BooleanSupplier emergencyStop, BooleanSupplier confirmHome,
                             BooleanSupplier started) {
        this.fire = fire;
        this.returnHome = returnHome;
        this.emergencyStop = emergencyStop;
        this.confirmHome = confirmHome;
        this.started = started;
    }
    @Override
    public void periodic() {
        try {
            readings.started = started.getAsBoolean();
            readings.fire = fire.getAsBoolean();
            readings.returnHome = returnHome.getAsBoolean();
            readings.emergencyStop = emergencyStop.getAsBoolean();
            readings.confirmHome = confirmHome.getAsBoolean();
            readings.sensorsHealthy = false;
            readings.left = leftMotor.getCurrentPosition();
            readings.right = rightMotor.getCurrentPosition();
            readings.leftCurrent = leftMotor.getCurrent(CurrentUnit.AMPS);
            readings.rightCurrent = rightMotor.getCurrent(CurrentUnit.AMPS);
            leftVelocity = leftMotor.getVelocity();
            rightVelocity = rightMotor.getVelocity();
            readings.sensorsHealthy = true;
            cycle.update(clock.getAsDouble(), readings);
            if (cycle.resetEncodersRequested()) {
                stopMotors();
                for (DcMotor m : new DcMotor[]{leftMotor, rightMotor}) {
                    m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                }
                readings.left = readings.right = 0;
            }
            if (cycle.power() == 0) stopMotors();
            else {
                leftMotor.setTargetPosition(cycle.target());
                rightMotor.setTargetPosition(cycle.target());
                if (leftMotor.getMode() != DcMotor.RunMode.RUN_TO_POSITION)
                    leftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                if (rightMotor.getMode() != DcMotor.RunMode.RUN_TO_POSITION)
                    rightMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                leftMotor.setPower(cycle.power());
                rightMotor.setPower(cycle.power());
            }
        } catch (RuntimeException exception) {
            readings.sensorsHealthy = false;
            cycle.cancel("Hardware/control error: " + exception.getClass().getSimpleName());
            stopMotors();
        }
    }
    public void addTelemetry(Telemetry telemetry) {
        telemetry.addData("Catapult state", cycle.state());
        telemetry.addData("Catapult status", cycle.reason());
        telemetry.addData("Catapult encoder L/R", "%d / %d", readings.left, readings.right);
        telemetry.addData("Catapult target", cycle.target());
        telemetry.addData("Catapult error L/R", "%d / %d",
                (long) cycle.target() - readings.left, (long) cycle.target() - readings.right);
        telemetry.addData("Catapult current A L/R", "%.2f / %.2f", readings.leftCurrent, readings.rightCurrent);
        telemetry.addData("Catapult velocity ticks/s L/R", "%.1f / %.1f", leftVelocity, rightVelocity);
        telemetry.addData("Catapult feedback", readings.sensorsHealthy ? "Available" : "Unavailable");
        telemetry.addData("Catapult requested power", cycle.power());
        telemetry.addData("Catapult INIT home", "After physical home: release, then gamepad 2 X + Y");
    }
    public void stop() {
        cycle.cancel("OpMode ended/cancelled");
        stopMotors();
    }
    private void stopMotors() {
        try { leftMotor.setPower(0); }
        finally { rightMotor.setPower(0); }
    }
}
