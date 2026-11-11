package org.firstinspires.ftc.teamcode.catapult;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

/**
 * Read-only inspection. Never writes power, direction, target, run mode, encoder reset or a servo.
 * Secure/unload the mechanism independently before enabling/running; this program cannot hold it.
 */
@Disabled
@TeleOp(name = "Catapult hardware check - READ ONLY", group = "Diagnostics")
public final class CatapultHardwareCheck extends OpMode {
    private static final String[] NAMES = { "catapultLeft", "catapultRight" };
    private final DcMotorEx[] motors = new DcMotorEx[NAMES.length];
    private final String[] errors = new String[NAMES.length];
    private final Integer[] first = new Integer[NAMES.length];
    private final int[] minimum = new int[NAMES.length], maximum = new int[NAMES.length];

    @Override public void init() {
        for (int i = 0; i < NAMES.length; i++) {
            try { motors[i] = hardwareMap.get(DcMotorEx.class, NAMES[i]); }
            catch (RuntimeException e) { errors[i] = e.getClass().getSimpleName() + ": " + e.getMessage(); }
        }
        report();
    }

    @Override public void init_loop() { report(); }
    @Override public void loop() { report(); }

    private void report() {
        telemetry.addLine("READ ONLY: does not load, hold, fire, reset encoders or change motor settings.");
        telemetry.addLine("Failed retaining spring: support/unload the catapult independently first.");
        for (int i = 0; i < NAMES.length; i++) {
            String name = NAMES[i];
            DcMotorEx motor = motors[i];
            if (motor == null) { telemetry.addData(name, errors[i]); continue; }
            try {
                MotorConfigurationType type = motor.getMotorType();
                telemetry.addData(name + " configured type", type == null ? "UNKNOWN" : type.getName());
                telemetry.addData(name + " configured ticks/rev", type == null ? "UNKNOWN" : type.getTicksPerRev());
                telemetry.addData(name + " configured ratio", type == null ? "UNKNOWN" : type.getGearing());
                telemetry.addData(name + " controller/port", "%s / %d", motor.getConnectionInfo(), motor.getPortNumber());
                telemetry.addData(name + " mode/direction/brake", "%s / %s / %s",
                        motor.getMode(), motor.getDirection(), motor.getZeroPowerBehavior());
                int ticks = motor.getCurrentPosition();
                if (first[i] == null) { first[i] = ticks; minimum[i] = ticks; maximum[i] = ticks; }
                minimum[i] = Math.min(minimum[i], ticks);
                maximum[i] = Math.max(maximum[i], ticks);
                telemetry.addData(name + " encoder", "now=%d delta=%d observed=[%d,%d]",
                        ticks, (long) ticks - first[i], minimum[i], maximum[i]);
                telemetry.addData(name + " requested power", motor.getPower());
            } catch (RuntimeException e) {
                telemetry.addData(name + " read failure", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            // Unsupported extended readings do not hide the encoder/configuration readings above.
            try { telemetry.addData(name + " velocity ticks/s", motor.getVelocity()); }
            catch (RuntimeException e) { telemetry.addData(name + " velocity", "UNAVAILABLE"); }
            try { telemetry.addData(name + " current A", motor.getCurrent(CurrentUnit.AMPS)); }
            catch (RuntimeException e) { telemetry.addData(name + " current", "UNAVAILABLE"); }
            try { telemetry.addData(name + " existing current alert A", motor.getCurrentAlert(CurrentUnit.AMPS)); }
            catch (RuntimeException e) { telemetry.addData(name + " current alert", "UNAVAILABLE"); }
        }
        telemetry.addLine("Configured motor type is a software setting, not physical identification.");
        telemetry.addLine("A static encoder count does not prove the encoder is connected or disconnected.");
        telemetry.update();
    }
}
