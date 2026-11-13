package org.firstinspires.ftc.teamcode.catapult;

/** Hardware-independent position cycle. Zero output is NOT mechanical retention. */
public final class CatapultCycle {
    public enum State { DISABLED, UNHOMED, READY, FIRING, RETURNING, FAULT }
    /** Fill with verified measurements before enabling. The controller takes its own copy. */
    public static final class Config {
        public boolean enabled, safeAtZeroPowerVerified;
        public int home, fire, minimum, maximum, tolerance, maximumSkew, minimumProgress;
        public double launchPower, returnPower, maximumCurrent;
        public double fireTimeout, returnTimeout, progressTimeout, settleTime;
        public String problem() {
            if (!enabled) return "Motion disabled: hardware settings unverified";
            if (!safeAtZeroPowerVerified) return "Zero-power mechanical state not verified";
            if (home != 0 || minimum >= maximum || home < minimum || home > maximum
                    || fire < minimum || fire > maximum || tolerance <= 0
                    || Math.abs((long) fire - home) <= 2L * tolerance
                    || maximumSkew <= 0 || minimumProgress <= 0) return "Invalid travel settings";
            if (!positive(launchPower) || launchPower > 1 || !positive(returnPower)
                    || returnPower > 1 || !positive(maximumCurrent)
                    || !positive(fireTimeout) || !positive(returnTimeout)
                    || !positive(progressTimeout) || !positive(settleTime)
                    || settleTime >= fireTimeout || settleTime >= returnTimeout)
                return "Invalid power/current/time settings";
            return "";
        }
        private Config copy() {
            Config c = new Config();
            c.enabled = enabled; c.safeAtZeroPowerVerified = safeAtZeroPowerVerified;
            c.home = home; c.fire = fire; c.minimum = minimum; c.maximum = maximum;
            c.tolerance = tolerance; c.maximumSkew = maximumSkew; c.minimumProgress = minimumProgress;
            c.launchPower = launchPower; c.returnPower = returnPower; c.maximumCurrent = maximumCurrent;
            c.fireTimeout = fireTimeout; c.returnTimeout = returnTimeout;
            c.progressTimeout = progressTimeout; c.settleTime = settleTime;
            return c;
        }
    }
    public static final class Inputs {
        public boolean started, fire, returnHome, emergencyStop, confirmHome, sensorsHealthy;
        public int left, right;
        public double leftCurrent, rightCurrent;
    }
    private final Config config;
    private State state;
    private String reason;
    private boolean previousFire, previousReturn, previousConfirm, neutralSeen, confirmNeutralSeen;
    private boolean resetEncoders;
    private double lastTime = Double.NaN, entered, leftProgressAt, rightProgressAt;
    private double settledSince = Double.NaN;
    private long bestLeftError, bestRightError;
    public CatapultCycle(Config config) {
        if (config == null) throw new IllegalArgumentException("Config required");
        this.config = config.copy();
        reason = this.config.problem();
        state = reason.isEmpty() ? State.UNHOMED : State.DISABLED;
    }
    public void update(double now, Inputs in) {
        resetEncoders = false;
        if (state == State.DISABLED || state == State.FAULT) return;
        if (in == null || !finite(now) || (!Double.isNaN(lastTime) && now < lastTime)) {
            cancel("Invalid input or clock"); return;
        }
        lastTime = now;
        boolean fireEdge = in.fire && !previousFire;
        boolean returnEdge = in.returnHome && !previousReturn;
        boolean confirmEdge = in.confirmHome && !previousConfirm;
        previousFire = in.fire;
        previousReturn = in.returnHome;
        previousConfirm = in.confirmHome;
        if (in.emergencyStop) { cancel("Emergency stop"); return; }
        if (!in.sensorsHealthy || !finite(in.leftCurrent) || !finite(in.rightCurrent)
                || in.leftCurrent < 0 || in.rightCurrent < 0) {
            cancel("Motor feedback unavailable"); return;
        }
        if (in.leftCurrent > config.maximumCurrent || in.rightCurrent > config.maximumCurrent) {
            cancel("Overcurrent"); return;
        }
        if (in.fire && in.returnHome) { cancel("Conflicting catapult controls"); return; }
        if (!in.confirmHome) confirmNeutralSeen = true;
        if (!in.started) {
            if (moving()) { cancel("OpMode no longer active"); return; }
            // Operator must physically establish home; this is not a position sensor.
            if (state == State.UNHOMED && confirmNeutralSeen && confirmEdge && !in.fire && !in.returnHome) {
                resetEncoders = true;
                state = State.READY;
                neutralSeen = false;
            }
            return;
        }
        if (state == State.UNHOMED) return;
        if (in.left < config.minimum || in.left > config.maximum
                || in.right < config.minimum || in.right > config.maximum) {
            cancel("Travel limit exceeded"); return;
        }
        if (Math.abs((long) in.left - in.right) > config.maximumSkew) {
            cancel("Motor positions disagree"); return;
        }
        if (state == State.READY) {
            if (!in.fire && !in.returnHome) neutralSeen = true;
            if (neutralSeen && fireEdge) {
                if (!at(in, config.home)) { cancel("Fire requested away from home"); return; }
                enter(State.FIRING, now, in);
            } else if (neutralSeen && returnEdge) enter(State.RETURNING, now, in);
            return;
        }
        if (state == State.FIRING && returnEdge) {
            cancel("Return requested during firing"); return;
        }
        double timeout = state == State.FIRING ? config.fireTimeout : config.returnTimeout;
        if (now - entered >= timeout) {
            cancel(state == State.FIRING ? "Firing timeout" : "Return timeout"); return;
        }
        int target = target();
        long leftError = Math.abs((long) in.left - target);
        long rightError = Math.abs((long) in.right - target);
        if (leftError <= config.tolerance || bestLeftError - leftError >= config.minimumProgress) {
            bestLeftError = leftError; leftProgressAt = now;
        }
        if (rightError <= config.tolerance || bestRightError - rightError >= config.minimumProgress) {
            bestRightError = rightError; rightProgressAt = now;
        }
        if (now - leftProgressAt >= config.progressTimeout || now - rightProgressAt >= config.progressTimeout) {
            cancel("Encoder progress timeout"); return;
        }
        if (at(in, target)) {
            if (Double.isNaN(settledSince)) settledSince = now;
            if (now - settledSince >= config.settleTime) {
                if (state == State.FIRING) enter(State.RETURNING, now, in);
                else { state = State.READY; neutralSeen = false; }
            }
        } else settledSince = Double.NaN;
    }
    private void enter(State next, double now, Inputs in) {
        state = next;
        entered = leftProgressAt = rightProgressAt = now;
        settledSince = Double.NaN;
        bestLeftError = Math.abs((long) in.left - target());
        bestRightError = Math.abs((long) in.right - target());
    }
    private boolean at(Inputs in, int target) {
        return Math.abs((long) in.left - target) <= config.tolerance
                && Math.abs((long) in.right - target) <= config.tolerance;
    }
    public void cancel(String reason) {
        if (state == State.FAULT) return;
        this.reason = reason;
        state = State.FAULT;
        resetEncoders = false;
    }
    public State state() { return state; }
    public String reason() { return reason; }
    public boolean resetEncodersRequested() { return resetEncoders; }
    public boolean moving() { return state == State.FIRING || state == State.RETURNING; }
    public int target() { return state == State.FIRING ? config.fire : config.home; }
    public double power() {
        return state == State.FIRING ? config.launchPower : state == State.RETURNING ? config.returnPower : 0;
    }
    private static boolean finite(double value) { return !Double.isNaN(value) && !Double.isInfinite(value); }
    private static boolean positive(double value) { return finite(value) && value > 0; }
}
