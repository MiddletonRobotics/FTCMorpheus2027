package org.firstinspires.ftc.teamcode.autonomous;

/** Hardware-independent sequence shared by the OpMode and desktop tests. Times are monotonic seconds. */
public final class DrivePauseParkSequence {
    public enum State { IDLE, TO_SHOOT, SHOOTING_PAUSE, TO_PARK, DONE, STOPPED, FAULT }
    public enum Leg { SHOOT, PARK }

    public interface Drive {
        void follow(Leg leg);
        void update();
        boolean busy();
        boolean atGoal(Leg leg);
        boolean stationary();
        boolean healthy();
        /** Must cancel following AND immediately zero motor outputs. */
        void stop();
    }

    private final Drive drive;
    private final double pauseSeconds, pathTimeoutSeconds, settleSeconds;
    private State state = State.IDLE;
    private double entered, lastTime, settledSince = Double.NaN;
    private String fault = "";

    public DrivePauseParkSequence(Drive drive, double pauseSeconds,
                                 double pathTimeoutSeconds, double settleSeconds) {
        if (drive == null || !finite(pauseSeconds) || pauseSeconds < 0
                || !finite(pathTimeoutSeconds) || pathTimeoutSeconds <= 0
                || !finite(settleSeconds) || settleSeconds < 0) {
            throw new IllegalArgumentException("Invalid sequence settings");
        }
        this.drive = drive;
        this.pauseSeconds = pauseSeconds;
        this.pathTimeoutSeconds = pathTimeoutSeconds;
        this.settleSeconds = settleSeconds;
    }

    public void start(double now) {
        if (state != State.IDLE) throw new IllegalStateException("Create a new sequence to restart");
        if (!finite(now)) throw new IllegalArgumentException("Invalid clock");
        lastTime = now;
        try {
            drive.stop();
            if (!drive.healthy()) { fail("Invalid localization", now); return; }
            enter(State.TO_SHOOT, now);
            drive.follow(Leg.SHOOT);
        } catch (RuntimeException e) { fail("Start: " + e.getClass().getSimpleName(), now); }
    }

    public void tick(double now, boolean emergencyStop) {
        if (emergencyStop) { stop(); return; }
        if (state == State.IDLE || terminal()) return;
        if (!finite(now) || now < lastTime) { fail("Invalid/backward clock", lastTime); return; }
        lastTime = now;
        try {
            // Exactly one follower/localizer update per active loop, including the stopped pause.
            drive.update();
            if (!drive.healthy()) { fail("Invalid localization", now); return; }
            if (state == State.SHOOTING_PAUSE) {
                drive.stop();
                if (!drive.atGoal(Leg.SHOOT) || !drive.stationary()) {
                    fail("Robot drifted during shooting pause", now);
                } else if (now >= entered + pauseSeconds) {
                    enter(State.TO_PARK, now);
                    drive.follow(Leg.PARK);
                }
                return;
            }
            Leg leg = state == State.TO_SHOOT ? Leg.SHOOT : Leg.PARK;
            // Pedro can become not-busy because of an endpoint timeout. Check measured pose/speed too.
            if (!drive.busy()) {
                if (!drive.atGoal(leg)) { fail("Path ended outside pose tolerance", now); return; }
                if (drive.stationary()) {
                    if (Double.isNaN(settledSince)) settledSince = now;
                    if (now - settledSince >= settleSeconds) {
                        drive.stop();
                        enter(leg == Leg.SHOOT ? State.SHOOTING_PAUSE : State.DONE, now);
                        return;
                    }
                } else settledSince = Double.NaN;
            } else settledSince = Double.NaN;
            if (now - entered >= pathTimeoutSeconds) fail("Path/settling timeout", now);
        } catch (RuntimeException e) { fail("Drive: " + e.getClass().getSimpleName(), now); }
    }

    public void stop() {
        if (state != State.FAULT && state != State.DONE) state = State.STOPPED;
        drive.stop();
    }

    private void fail(String reason, double now) {
        fault = reason;
        enter(State.FAULT, now);
        drive.stop();
    }

    private void enter(State next, double now) {
        state = next;
        entered = now;
        settledSince = Double.NaN;
    }

    private boolean terminal() { return state == State.DONE || state == State.STOPPED || state == State.FAULT; }
    private static boolean finite(double v) { return !Double.isNaN(v) && !Double.isInfinite(v); }
    public State state() { return state; }
    public String fault() { return fault; }
}
