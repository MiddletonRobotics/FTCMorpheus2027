import org.firstinspires.ftc.teamcode.catapult.CatapultManualControl;

/** Tests the production manual controller, not a second implementation of its behavior. */
public final class CatapultManualChecks {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static CatapultManualControl ready() {
        CatapultManualControl c = new CatapultManualControl();
        check(c.update(0, false, false, false) == 0, "neutral starts stopped");
        return c;
    }

    public static void main(String[] args) {
        CatapultManualControl c = new CatapultManualControl();
        check(c.update(0, true, false, false) == 0, "startup-held bumper cannot move");
        check(c.update(.1, false, true, false) == 0, "startup-held trigger cannot fire");
        check(c.update(.2, false, false, false) == 0, "neutral arms without moving");
        check(c.update(.3, true, false, false) == -1, "bumper commands existing down polarity");
        check(c.update(.4, false, false, false) == 0, "bumper release stops immediately");
        check(c.update(.5, true, false, false) == 0, "must observe neutral after stop");
        c.update(.6, false, false, false);
        check(c.update(.7, true, false, false) == -1, "fresh bumper press can lower");

        c = ready(); c.update(.1, true, false, false);
        check(c.update(.899, true, false, false) == -1, "lower burst continues before deadline");
        check(c.update(.9, true, false, false) == 0, "lower stops at 800 ms");
        check(c.update(100, true, false, false) == 0, "held bumper never repeats timed burst");
        c.update(100.1, false, false, false);
        check(c.update(100.2, true, false, false) == -1, "release is required before next burst");

        c = ready(); c.update(.1, true, false, false);
        check(c.update(.2, true, true, false) == 0, "conflicting controls stop both actions");
        check(c.update(.3, false, true, false) == 0, "releasing lower cannot accidentally launch");
        c.update(.4, false, false, false);
        check(c.update(.5, false, true, false) == 1, "fresh trigger preserves launch action");
        check(c.update(.6, true, false, false) == 0, "cannot reverse launch directly into lowering");
        check(c.update(.7, true, false, false) == 0, "held lower remains locked out");

        c = ready(); c.update(.1, false, true, false);
        check(c.update(.2, false, false, false) == 1, "existing launch is a single-press burst");
        check(c.update(.6, false, true, false) == 0, "launch stops at 500 ms");
        check(c.update(10, false, true, false) == 0, "held trigger cannot repeat launch");

        for (int phase = 0; phase < 3; phase++) {
            c = ready();
            if (phase == 1) c.update(.1, true, false, false);
            if (phase == 2) c.update(.1, false, true, false);
            check(c.update(.2, false, false, true) == 0, "BACK stops every state");
            check(c.update(.3, true, false, false) == 0, "BACK remains latched");
        }
        c = ready(); c.update(.1, true, false, false); c.stop();
        check(c.update(.2, true, false, false) == 0, "command interruption is latched off");
        for (double invalid : new double[] { Double.NaN, Double.POSITIVE_INFINITY, -.1 }) {
            c = ready(); c.update(.1, true, false, false);
            check(c.update(invalid, true, false, false) == 0, "bad/backward clock stops output");
            check(c.update(2, true, false, false) == 0, "clock fault cannot restart output");
        }
        System.out.println("PASS: " + checks + " manual catapult checks");
    }
}
