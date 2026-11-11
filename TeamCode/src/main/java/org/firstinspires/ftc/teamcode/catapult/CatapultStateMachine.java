package org.firstinspires.ftc.teamcode.catapult;

/**
 * Logic model ONLY: has no motor, servo, hardware-map or TeleOp binding. Requests are advisory.
 * A future reviewed adapter must retain stored energy mechanically on STOP, brownout and faults.
 */
public final class CatapultStateMachine {
    public enum State { IDLE, LOADING, HELD, FIRING, RECOVERING, FAULT }

    public static final class Inputs {
        public boolean loadHeld, firePressed, emergencyStop;
        public boolean hardwareVerified, sensorsHealthy;
        public boolean retentionConfirmed, fullyLoaded, home, shotComplete;
        /** Future adapter ORs both motors' measured current alarms, limits and synchronization faults. */
        public boolean overCurrent, limitExceeded;
        /** A known failed retaining spring/latch blocks loading and firing, including from IDLE. */
        public boolean holdingMechanismFault;
    }

    private final double loadTimeout, fireTimeout, recoveryTimeout;
    private State state = State.IDLE;
    private double entered, lastTime = Double.NaN;
    private boolean previousLoad, previousFire, neutralSeen, fireRearmed;
    private String fault = "";

    public CatapultStateMachine(double loadTimeout, double fireTimeout, double recoveryTimeout) {
        if (!positive(loadTimeout) || !positive(fireTimeout) || !positive(recoveryTimeout)) {
            throw new IllegalArgumentException("Timeouts must be finite and positive");
        }
        this.loadTimeout = loadTimeout;
        this.fireTimeout = fireTimeout;
        this.recoveryTimeout = recoveryTimeout;
    }

    public void update(double now, Inputs in) {
        if (state == State.FAULT) return; // Latched; no controller button clears a stored-energy fault.
        if (in == null || !finite(now) || (!Double.isNaN(lastTime) && now < lastTime)) {
            fail("Invalid input/clock"); return;
        }
        lastTime = now;
        boolean loadEdge = in.loadHeld && !previousLoad;
        boolean fireEdge = in.firePressed && !previousFire;
        previousLoad = in.loadHeld;
        previousFire = in.firePressed;
        if (in.emergencyStop) { fail("Emergency stop"); return; }
        if (in.holdingMechanismFault) { fail("Holding mechanism failed"); return; }
        if (in.overCurrent || in.limitExceeded) { fail("Current/travel/synchronization limit"); return; }
        if (!in.hardwareVerified || !in.sensorsHealthy) { fail("Hardware or sensors not verified"); return; }

        switch (state) {
            case IDLE:
                // Must observe safe home and released controls before accepting a fresh load edge.
                if (!in.home) { fail("Not at safe home"); break; }
                if (!in.loadHeld && !in.firePressed) neutralSeen = true;
                if (neutralSeen && loadEdge && !in.firePressed) enter(State.LOADING, now);
                break;
            case LOADING:
                if (now - entered >= loadTimeout) { fail("Loading timeout"); break; }
                if (in.fullyLoaded || !in.loadHeld) {
                    // Never interpret zero motor power or BRAKE as retention confirmation.
                    if (!in.retentionConfirmed) { fail("No confirmed mechanical retention"); break; }
                    enter(State.HELD, now);
                    fireRearmed = false;
                }
                break;
            case HELD:
                if (!in.retentionConfirmed) { fail("Retention lost"); break; }
                // A trigger held while loading cannot become an accidental fire command.
                if (!in.firePressed && !in.loadHeld) fireRearmed = true;
                if (fireRearmed && fireEdge && !in.loadHeld && in.fullyLoaded) {
                    enter(State.FIRING, now);
                } else if (loadEdge && !in.firePressed) {
                    enter(State.LOADING, now);
                }
                break;
            case FIRING:
                if (now - entered >= fireTimeout) { fail("Firing timeout"); break; }
                if (in.shotComplete) enter(State.RECOVERING, now);
                break;
            case RECOVERING:
                if (now - entered >= recoveryTimeout) { fail("Recovery timeout"); break; }
                // No automatic motor recovery is implemented. A future mechanism must prove safe home.
                if (in.home && !in.loadHeld && !in.firePressed) {
                    enter(State.IDLE, now);
                    neutralSeen = false;
                }
                break;
            default: break;
        }
    }

    private void enter(State next, double now) { state = next; entered = now; }
    private void fail(String reason) { state = State.FAULT; fault = reason; }
    private static boolean finite(double v) { return !Double.isNaN(v) && !Double.isInfinite(v); }
    private static boolean positive(double v) { return finite(v) && v > 0; }
    public State state() { return state; }
    public String fault() { return fault; }
    public boolean loadRequested() { return state == State.LOADING; }
    public boolean releaseRequested() { return state == State.FIRING; }
    public boolean retainRequested() { return state != State.FIRING; }
}
