package org.firstinspires.ftc.teamcode.autonomous;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** No catapult devices or commands. Enable only after hardware, route and localization validation. */
@Disabled
@Autonomous(name = "Morpheus Drive Pause Park", group = "Diagnostics")
public final class DrivePauseParkAuto extends OpMode {
    // Initial diagnostic settings, not a calibration or a safety certification.
    public static final double MAX_DRIVE_POWER = 0.25;
    public static final double PATH_TIMEOUT_SECONDS = 10.0;
    public static final double SETTLE_SECONDS = 0.25;
    public static final double POSITION_TOLERANCE_INCHES = 1.0;
    public static final double HEADING_TOLERANCE_RADIANS = Math.toRadians(3);
    public static final double STOP_SPEED_INCHES_PER_SECOND = 0.5;
    public static final double STOP_TURN_RADIANS_PER_SECOND = Math.toRadians(3);

    private final List<DcMotor> motors = new ArrayList<>();
    private Follower follower;
    private DrivePauseParkSequence sequence;
    private String initFault = "";

    @Override public void init() {
        try {
            // Keep references before follower construction, so partial initialization can be stopped.
            String[] names = { Constants.drivetrainConfig.frontLeftName.get(),
                    Constants.drivetrainConfig.backLeftName.get(),
                    Constants.drivetrainConfig.frontRightName.get(),
                    Constants.drivetrainConfig.backRightName.get() };
            for (String name : names) {
                DcMotor motor = hardwareMap.get(DcMotor.class, name);
                motors.add(motor);
                motor.setPower(0);
            }
            Follower base = Constants.createFollower(hardwareMap);
            follower = new Follower(base.localizer,
                    new CappedDrive(base.drivetrain, MAX_DRIVE_POWER), base.algorithm());
            follower.holdEnd.set(true);
            follower.setPose(DrivePauseParkRoute.start());
            sequence = new DrivePauseParkSequence(new PedroDrive(),
                    DrivePauseParkRoute.SHOOTING_DELAY_SECONDS, PATH_TIMEOUT_SECONDS, SETTLE_SECONDS);
        } catch (RuntimeException e) {
            initFault = "Initialization failed: " + e.getMessage();
        } finally { zeroMotors(); }
        report();
    }

    @Override public void init_loop() { report(); }
    @Override public void start() {
        if (sequence != null && initFault.isEmpty()) sequence.start(seconds());
        else zeroMotors();
    }
    @Override public void loop() {
        try {
            if (sequence != null && initFault.isEmpty()) sequence.tick(seconds(), gamepad1.back);
            else zeroMotors();
        } catch (RuntimeException e) {
            initFault = "Stopped after drive failure: " + e.getMessage();
            stop();
        }
        report();
    }
    @Override public void stop() {
        try {
            if (sequence != null) sequence.stop();
            else if (follower != null) follower.stop();
        } finally { zeroMotors(); }
    }

    private void zeroMotors() {
        // Try every motor even when one device write fails.
        for (DcMotor motor : motors) {
            try { motor.setPower(0); motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE); }
            catch (RuntimeException ignored) { initFault = "Motor stop write failed; use Driver Station STOP"; }
        }
    }
    private double seconds() { return System.nanoTime() / 1e9; }
    private void report() {
        telemetry.addData("State", sequence == null ? "NOT READY" : sequence.state());
        telemetry.addData("Fault", initFault.isEmpty() && sequence != null ? sequence.fault() : initFault);
        telemetry.addData("Shooting", "SIMULATED: %.2f seconds, no catapult", DrivePauseParkRoute.SHOOTING_DELAY_SECONDS);
        telemetry.addData("Stop", "Driver Station STOP or gamepad 1 BACK");
        if (follower != null) telemetry.addData("Pose", follower.pose());
        telemetry.update();
    }

    private final class PedroDrive implements DrivePauseParkSequence.Drive {
        private final Path shootingPath = DrivePauseParkRoute.toShoot();
        private final Path parkingPath = DrivePauseParkRoute.toPark();
        @Override public void follow(DrivePauseParkSequence.Leg leg) {
            follower.follow(leg == DrivePauseParkSequence.Leg.SHOOT
                    ? shootingPath : parkingPath);
        }
        @Override public void update() { follower.update(); }
        @Override public boolean busy() { return follower.isBusy(); }
        @Override public boolean atGoal(DrivePauseParkSequence.Leg leg) {
            Pose target = leg == DrivePauseParkSequence.Leg.SHOOT
                    ? shootingPath.endPose() : parkingPath.endPose();
            Pose actual = follower.pose();
            double headingError = Math.atan2(Math.sin(actual.heading() - target.heading()),
                    Math.cos(actual.heading() - target.heading()));
            return actual.distance(target) <= POSITION_TOLERANCE_INCHES
                    && Math.abs(headingError) <= HEADING_TOLERANCE_RADIANS;
        }
        @Override public boolean stationary() {
            Velocity v = follower.velocity();
            return v.linSpeed() <= STOP_SPEED_INCHES_PER_SECOND
                    && Math.abs(v.omega) <= STOP_TURN_RADIANS_PER_SECOND;
        }
        @Override public boolean healthy() {
            Pose p = follower.pose();
            Velocity v = follower.velocity();
            return finite(p.x()) && finite(p.y()) && finite(p.heading())
                    && finite(v.vx) && finite(v.vy) && finite(v.omega);
        }
        @Override public void stop() {
            try { follower.stop(); follower.drivetrain.stop(true); }
            finally { zeroMotors(); }
        }
    }

    private static boolean finite(double v) { return !Double.isNaN(v) && !Double.isInfinite(v); }

    /** Caps the sum of chassis demands, bounding each mecanum wheel's requested power. */
    private static final class CappedDrive implements Drivetrain {
        private final Drivetrain delegate;
        private final double cap;
        CappedDrive(Drivetrain delegate, double cap) {
            if (!finite(cap) || cap <= 0 || cap > 1) throw new IllegalArgumentException("Invalid power cap");
            this.delegate = delegate;
            this.cap = cap;
        }
        @Override public void drive(DrivePowers p, boolean manual) {
            if (!finite(p.forward()) || !finite(p.strafe()) || !finite(p.turn())) {
                delegate.stop(true);
                throw new IllegalStateException("Nonfinite drive demand");
            }
            double scale = Math.min(1, cap / Math.max(cap,
                    Math.abs(p.forward()) + Math.abs(p.strafe()) + Math.abs(p.turn())));
            delegate.drive(new DrivePowers(p.forward() * scale, p.strafe() * scale, p.turn() * scale), manual);
        }
        @Override public double maxScaling(DrivePowers a, DrivePowers b) { return delegate.maxScaling(a, b); }
        @Override public void stop() { delegate.stop(true); }
        @Override public void stop(boolean brake) { delegate.stop(true); }
        @Override public Map<String, Object> debug() { return delegate.debug(); }
        @Override public double interpolateVelocity(double x, double y, double h) {
            return delegate.interpolateVelocity(x, y, h);
        }
        @Override public double interpolateAcceleration(double x, double y, double h) {
            return delegate.interpolateAcceleration(x, y, h);
        }
    }
}
