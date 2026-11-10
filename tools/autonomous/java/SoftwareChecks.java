import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import org.firstinspires.ftc.teamcode.autonomous.DrivePauseParkRoute;
import org.firstinspires.ftc.teamcode.autonomous.DrivePauseParkSequence;
import org.firstinspires.ftc.teamcode.catapult.CatapultStateMachine;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

/** Executes production sequence and Pedro path geometry on the JVM; ideal motion, not a robot simulator. */
public final class SoftwareChecks {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    private static final class Drive implements DrivePauseParkSequence.Drive {
        double now, began;
        Path path;
        Pose pose = DrivePauseParkRoute.start();
        boolean stopped = true, stuck, badGoal, drift, badSensor, moving, throwUpdate;
        int starts, updates;
        double duration;
        public void follow(DrivePauseParkSequence.Leg leg) {
            path = leg == DrivePauseParkSequence.Leg.SHOOT ? DrivePauseParkRoute.toShoot() : DrivePauseParkRoute.toPark();
            began = now;
            double length = 0;
            Pose previous = path.get(0);
            for (int n = 1; n <= 200; n++) {
                Pose next = path.get(n / 200.0);
                length += previous.distance(next);
                previous = next;
            }
            duration = Math.max(length / 12, 0.02); // Ideal preview only; no velocity/physics prediction.
            starts++;
            stopped = false;
        }
        public void update() {
            updates++;
            if (throwUpdate) throw new IllegalStateException("Injected drive failure");
            if (!stopped && path != null) pose = path.get(Math.min(1, Math.max(0, (now - began) / duration)));
        }
        public boolean busy() { return !stopped && (stuck || now - began < duration); }
        public boolean atGoal(DrivePauseParkSequence.Leg leg) { return !badGoal && !drift; }
        public boolean stationary() { return !busy() && !moving; }
        public boolean healthy() { return !badSensor; }
        public void stop() { stopped = true; }
    }

    private static DrivePauseParkSequence sequence(Drive d) {
        DrivePauseParkSequence s = new DrivePauseParkSequence(d, 3, 10, .25);
        s.start(0);
        return s;
    }
    private static void tick(Drive d, DrivePauseParkSequence s, double time) {
        d.now = time;
        s.tick(time, false);
    }
    private static double reachPause(Drive d, DrivePauseParkSequence s) {
        for (int n = 1; n < 500; n++) {
            tick(d, s, n * .02);
            if (s.state() == DrivePauseParkSequence.State.SHOOTING_PAUSE) return d.now;
        }
        throw new AssertionError("Did not reach shooting pause");
    }

    private static void autonomousChecks(String traceFile) throws Exception {
        check(DrivePauseParkRoute.start().distance(DrivePauseParkRoute.toShoot().get(0)) < 1e-8, "start continuity");
        check(DrivePauseParkRoute.toShoot().endPose().distance(DrivePauseParkRoute.toPark().get(0)) < 1e-8, "path continuity");
        check(DrivePauseParkRoute.SHOOTING_DELAY_SECONDS >= 0, "configured pause is nonnegative");
        Drive d = new Drive();
        DrivePauseParkSequence s = sequence(d);
        double pause = reachPause(d, s);
        check(d.stopped && d.starts == 1, "motors zero before wait");
        tick(d, s, pause + 2.999);
        check(s.state() == DrivePauseParkSequence.State.SHOOTING_PAUSE && d.stopped, "no early departure");
        tick(d, s, pause + 3);
        check(s.state() == DrivePauseParkSequence.State.TO_PARK && d.starts == 2, "park starts after full pause");
        for (int n = 1; n < 500 && s.state() != DrivePauseParkSequence.State.DONE; n++) tick(d, s, pause + 3 + n * .02);
        check(s.state() == DrivePauseParkSequence.State.DONE && d.stopped, "finished stopped");

        d = new Drive(); s = sequence(d); d.stuck = true; tick(d, s, 10);
        check(s.state() == DrivePauseParkSequence.State.FAULT && d.stopped && d.starts == 1, "first path timeout");
        d = new Drive(); s = sequence(d); pause = reachPause(d, s); tick(d, s, pause + 3); d.stuck = true; tick(d, s, pause + 13);
        check(s.state() == DrivePauseParkSequence.State.FAULT && d.stopped, "park path timeout");
        d = new Drive(); s = sequence(d); d.badGoal = true; tick(d, s, 5);
        check(s.state() == DrivePauseParkSequence.State.FAULT && d.starts == 1, "not-busy outside goal is failure");
        d = new Drive(); s = sequence(d); d.moving = true;
        for (int n = 1; n <= 500; n++) tick(d, s, n * .02);
        check(s.state() == DrivePauseParkSequence.State.FAULT, "cannot enter wait while moving");
        d = new Drive(); s = sequence(d); pause = reachPause(d, s); d.drift = true; tick(d, s, pause + .02);
        check(s.state() == DrivePauseParkSequence.State.FAULT && d.stopped && d.starts == 1, "pause drift aborts park");
        for (int phase = 0; phase < 3; phase++) {
            d = new Drive(); s = sequence(d);
            if (phase > 0) { pause = reachPause(d, s); if (phase == 2) tick(d, s, pause + 3); }
            s.tick(d.now, true);
            check(s.state() == DrivePauseParkSequence.State.STOPPED && d.stopped, "emergency stop in every phase");
            int starts = d.starts;
            tick(d, s, 100);
            check(d.starts == starts, "stopped sequence never restarts");
        }
        d = new Drive(); s = sequence(d); d.badSensor = true; tick(d, s, .02);
        check(s.state() == DrivePauseParkSequence.State.FAULT && d.stopped, "sensor fault");
        d = new Drive(); s = sequence(d); d.throwUpdate = true; tick(d, s, .02);
        check(s.state() == DrivePauseParkSequence.State.FAULT && d.stopped, "drive exception");
        d = new Drive(); s = sequence(d); tick(d, s, Double.NaN);
        check(s.state() == DrivePauseParkSequence.State.FAULT && d.stopped, "bad clock");

        // Export one complete run from the ACTUAL Java sequence and ACTUAL Pedro 3 paths.
        d = new Drive();
        s = new DrivePauseParkSequence(d, DrivePauseParkRoute.SHOOTING_DELAY_SECONDS, 10, .25);
        s.start(0);
        Files.createDirectories(Paths.get(traceFile).getParent());
        try (PrintWriter out = new PrintWriter(traceFile, "UTF-8")) {
            out.println("seconds,state,x,y,headingDegrees,outputsStopped");
            for (int n = 0; n <= 1500; n++) {
                tick(d, s, n * .02);
                out.printf(Locale.ROOT, "%.3f,%s,%.6f,%.6f,%.6f,%s%n", d.now, s.state(),
                        d.pose.x(), d.pose.y(), Math.toDegrees(d.pose.heading()), d.stopped);
                if (s.state() == DrivePauseParkSequence.State.DONE) break;
            }
        }
        check(s.state() == DrivePauseParkSequence.State.DONE, "preview run completes");
    }

    private static CatapultStateMachine.Inputs inputs() {
        CatapultStateMachine.Inputs in = new CatapultStateMachine.Inputs();
        in.hardwareVerified = true; in.sensorsHealthy = true; in.home = true;
        return in;
    }
    private static CatapultStateMachine cat() { return new CatapultStateMachine(5, 1, 5); }
    private static void load(CatapultStateMachine c, CatapultStateMachine.Inputs in) {
        c.update(0, in); in.loadHeld = true; c.update(.1, in);
        check(c.state() == CatapultStateMachine.State.LOADING, "fresh load edge");
    }
    private static void hold(CatapultStateMachine c, CatapultStateMachine.Inputs in) {
        load(c, in); in.loadHeld = false; in.retentionConfirmed = true; in.fullyLoaded = true; c.update(.2, in);
        check(c.state() == CatapultStateMachine.State.HELD && !c.loadRequested() && c.retainRequested(), "held without loading power request");
    }
    private static void fire(CatapultStateMachine c, CatapultStateMachine.Inputs in) {
        hold(c, in); c.update(.3, in); in.firePressed = true; c.update(.4, in);
        check(c.state() == CatapultStateMachine.State.FIRING && c.releaseRequested(), "deliberate fresh fire");
    }
    private static void catapultChecks() {
        CatapultStateMachine c = cat(); CatapultStateMachine.Inputs in = inputs();
        fire(c, in); in.shotComplete = true; c.update(.5, in);
        check(c.state() == CatapultStateMachine.State.RECOVERING && !c.releaseRequested(), "bounded release ends after shot");
        in.firePressed = false; c.update(.6, in);
        check(c.state() == CatapultStateMachine.State.IDLE, "safe home recovery");

        c = cat(); in = inputs(); in.loadHeld = true; c.update(0, in);
        check(!c.loadRequested(), "startup held button ignored");
        c = cat(); in = inputs(); load(c, in); in.loadHeld = false; c.update(.2, in);
        check(c.state() == CatapultStateMachine.State.FAULT && c.retainRequested(), "release without retention faults");
        c = cat(); in = inputs(); load(c, in); in.firePressed = true; in.fullyLoaded = true; in.retentionConfirmed = true; c.update(.2, in);
        in.loadHeld = false; c.update(.3, in);
        check(!c.releaseRequested(), "fire held through loading ignored");
        c = cat(); in = inputs(); hold(c, in); in.fullyLoaded = false; c.update(.3, in); in.firePressed = true; c.update(.4, in);
        check(!c.releaseRequested(), "partly loaded cannot fire");
        c = cat(); in = inputs(); hold(c, in); in.retentionConfirmed = false; c.update(.3, in);
        check(c.state() == CatapultStateMachine.State.FAULT, "retention loss faults");
        c = cat(); in = inputs(); load(c, in); c.update(5.1, in);
        check(c.state() == CatapultStateMachine.State.FAULT, "loading timeout");
        c = cat(); in = inputs(); fire(c, in); c.update(1.41, in);
        check(c.state() == CatapultStateMachine.State.FAULT && !c.releaseRequested(), "firing timeout");
        c = cat(); in = inputs(); fire(c, in); in.shotComplete = true; c.update(.5, in); in.home = false; c.update(5.5, in);
        check(c.state() == CatapultStateMachine.State.FAULT, "recovery timeout");
        for (int phase = 0; phase < 5; phase++) {
            for (int fault = 0; fault < 5; fault++) {
                c = cat(); in = inputs();
                if (phase == 1) load(c, in);
                if (phase == 2) hold(c, in);
                if (phase >= 3) fire(c, in);
                if (phase == 4) { in.shotComplete = true; c.update(.5, in); }
                if (fault == 0) in.emergencyStop = true;
                if (fault == 1) in.overCurrent = true;
                if (fault == 2) in.limitExceeded = true;
                if (fault == 3) in.sensorsHealthy = false;
                if (fault == 4) in.hardwareVerified = false;
                c.update(.6, in);
                check(c.state() == CatapultStateMachine.State.FAULT && !c.loadRequested()
                        && !c.releaseRequested() && c.retainRequested(), "fault suppresses loading/release in every state");
                in = inputs(); c.update(.7, in);
                check(c.state() == CatapultStateMachine.State.FAULT, "fault is latched");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        autonomousChecks(args[0]);
        catapultChecks();
        System.out.println("PASS: " + checks + " Java checks; trace: " + args[0]);
    }
}
