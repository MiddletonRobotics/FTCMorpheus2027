package org.firstinspires.ftc.teamcode.catapult;

/** Bounded manual lowering, not an encoder hold or a mechanical retaining device. */
public final class CatapultManualControl {
    // Preserve the directions, power and maximum durations already used by TeleOpBindings.
    public static final double LOWER_POWER = -1.0;
    public static final double LOWER_SECONDS = 0.8;
    public static final double LAUNCH_POWER = 1.0;
    public static final double LAUNCH_SECONDS = 0.5;

    public enum State { RELEASE_CONTROLS, IDLE, LOWERING, LAUNCHING, STOPPED }
    private State state = State.RELEASE_CONTROLS;
    private double began, lastTime = Double.NaN;

    public double update(double now, boolean lowerHeld, boolean launchPressed, boolean emergencyStop) {
        if (emergencyStop || !finite(now) || (!Double.isNaN(lastTime) && now < lastTime)) stop();
        lastTime = now;
        if (state == State.STOPPED) return 0;
        if (lowerHeld && launchPressed) {
            state = State.RELEASE_CONTROLS;
            return 0;
        }
        switch (state) {
            case RELEASE_CONTROLS:
                if (!lowerHeld && !launchPressed) state = State.IDLE;
                return 0;
            case IDLE:
                if (lowerHeld) { state = State.LOWERING; began = now; return LOWER_POWER; }
                if (launchPressed) { state = State.LAUNCHING; began = now; return LAUNCH_POWER; }
                return 0;
            case LOWERING:
                if (!lowerHeld || launchPressed || now >= began + LOWER_SECONDS) {
                    state = State.RELEASE_CONTROLS;
                    return 0;
                }
                return LOWER_POWER;
            case LAUNCHING:
                // Preserve the previous single-press, 500 ms shot. Never reverse directly into lowering.
                if (lowerHeld || now >= began + LAUNCH_SECONDS) {
                    state = State.RELEASE_CONTROLS;
                    return 0;
                }
                return LAUNCH_POWER;
            default: return 0;
        }
    }

    public void stop() { state = State.STOPPED; }
    public State state() { return state; }
    private static boolean finite(double v) { return !Double.isNaN(v) && !Double.isInfinite(v); }
}
