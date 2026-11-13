import org.firstinspires.ftc.teamcode.catapult.CatapultCycle;
import org.firstinspires.ftc.teamcode.catapult.CatapultSettings;
import java.util.function.Consumer;
import static org.firstinspires.ftc.teamcode.catapult.CatapultCycle.State.*;

public final class CatapultCycleChecks {
    static int checks;
    static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    public static CatapultCycle.Config fixtureConfig() {
        CatapultCycle.Config c = new CatapultCycle.Config();
        c.enabled = c.safeAtZeroPowerVerified = true;
        c.home = 0; c.fire = 100; c.minimum = -10; c.maximum = 110;
        c.tolerance = 3; c.maximumSkew = 15; c.minimumProgress = 2;
        c.launchPower = .8; c.returnPower = .3; c.maximumCurrent = 3;
        c.fireTimeout = 2; c.returnTimeout = 3; c.progressTimeout = .4; c.settleTime = .1;
        return c; // Illustrative test numbers, NOT hardware calibration.
    }
    static class Fixture {
        final CatapultCycle cycle;
        final CatapultCycle.Inputs in = new CatapultCycle.Inputs();
        double now;
        Fixture() { this(fixtureConfig()); }
        Fixture(CatapultCycle.Config c) { cycle = new CatapultCycle(c); in.sensorsHealthy = true; }
        void step(double dt) { now += dt; cycle.update(now, in); }
        void home() {
            step(0); in.confirmHome = true; step(.01);
            check(cycle.resetEncodersRequested(), "home confirmation emits reset once");
            in.confirmHome = false; in.left = in.right = 0; in.started = true; step(.01);
            check(!cycle.resetEncodersRequested() && cycle.state() == READY, "homed and neutral");
        }
        void fire() { in.fire = true; step(.01); check(cycle.state() == FIRING, "fire starts"); }
        void reachFire() {
            in.left = in.right = 100; step(.1); step(.11);
            check(cycle.state() == RETURNING, "settled fire begins return");
        }
        void fault(String reason) {
            check(cycle.state() == FAULT && cycle.reason().equals(reason), reason);
            check(cycle.power() == 0 && !cycle.resetEncodersRequested(), "fault suppresses motor/reset requests");
        }
    }
    public static void main(String[] args) {
        Fixture f = new Fixture(CatapultSettings.measuredConfiguration());
        f.in.fire = f.in.returnHome = f.in.confirmHome = true; f.step(1);
        check(f.cycle.state() == DISABLED && f.cycle.power() == 0 && !f.cycle.resetEncodersRequested(), "production config disables movement and reset");
        java.util.List<Consumer<CatapultCycle.Config>> invalid = java.util.Arrays.asList(
            c -> c.safeAtZeroPowerVerified = false,
            c -> c.maximum = -1,
            c -> c.fire = 200,
            c -> c.tolerance = 0,
            c -> c.fire = 5,
            c -> c.maximumSkew = 0,
            c -> c.minimumProgress = 0,
            c -> c.launchPower = 1.1,
            c -> c.returnPower = Double.NaN,
            c -> c.maximumCurrent = 0,
            c -> c.fireTimeout = 0,
            c -> c.returnTimeout = 0,
            c -> c.progressTimeout = 0,
            c -> c.settleTime = 3,
            c -> c.home = 1
        );
        for (Consumer<CatapultCycle.Config> change : invalid) {
            CatapultCycle.Config c = fixtureConfig(); change.accept(c);
            check(new CatapultCycle(c).state() == DISABLED, "invalid config blocks motion");
        }
        f = new Fixture(); f.in.left = 75; f.in.right = 90; f.step(0);
        check(f.cycle.state() == UNHOMED && !f.cycle.resetEncodersRequested(), "init does not assert home or zero encoders");
        f.in.started = f.in.fire = f.in.confirmHome = true; f.step(.01);
        check(f.cycle.state() == UNHOMED && f.cycle.power() == 0, "cannot home or fire after START when unhomed");
        f = new Fixture(); f.in.confirmHome = true; f.step(0);
        check(!f.cycle.resetEncodersRequested(), "held home confirmation at boot is ignored");
        f.in.confirmHome = false; f.step(.01); f.in.confirmHome = true; f.step(.01);
        check(f.cycle.resetEncodersRequested(), "fresh INIT confirmation accepted");
        f = new Fixture(); f.home(); f.fire();
        check(f.cycle.power() == .8 && f.cycle.target() == 100, "fire output");
        f.reachFire(); check(f.cycle.power() == .3 && f.cycle.target() == 0, "return output");
        f.in.fire = false; f.step(.01); f.in.fire = true; f.in.left = f.in.right = 80; f.step(.1);
        check(f.cycle.state() == RETURNING, "fresh fire during return is ignored");
        f.in.left = f.in.right = 0; f.step(.1); f.step(.11);
        check(f.cycle.state() == READY && f.cycle.power() == 0, "home completion zeroes output");
        f.step(.1); check(f.cycle.state() == READY, "held trigger does not repeat");
        f.in.fire = false; f.step(.01); f.fire();
        f.cycle.cancel("Cancelled"); f.fault("Cancelled"); f.in.fire = false; f.in.confirmHome = true; f.step(.1);
        f.fault("Cancelled");
        CatapultCycle.Config c = fixtureConfig(); c.progressTimeout = 10;
        f = new Fixture(c); f.home(); f.fire(); f.step(2.01); f.fault("Firing timeout");
        f = new Fixture(c); f.home(); f.fire(); f.reachFire(); f.step(3.01); f.fault("Return timeout");
        f = new Fixture(); f.home(); f.fire(); f.step(.41); f.fault("Encoder progress timeout");
        c = fixtureConfig(); c.maximumSkew = 105;
        f = new Fixture(c); f.home(); f.fire(); f.in.left = 100; f.step(.2); f.step(.21); f.fault("Encoder progress timeout");
        f = new Fixture(); f.home(); f.fire(); f.in.left = 20; f.step(.01); f.fault("Motor positions disagree");
        f = new Fixture(); f.home(); f.in.left = f.in.right = 111; f.step(.01); f.fault("Travel limit exceeded");
        for (int side = 0; side < 2; side++) {
            f = new Fixture(); f.home(); f.fire();
            if (side == 0) f.in.leftCurrent = 3.01; else f.in.rightCurrent = 3.01;
            f.step(.01); f.fault("Overcurrent");
        }
        f = new Fixture(); f.home(); f.in.rightCurrent = Double.NaN; f.step(.01); f.fault("Motor feedback unavailable");
        f = new Fixture(); f.home(); f.in.sensorsHealthy = false; f.step(.01); f.fault("Motor feedback unavailable");
        f = new Fixture(); f.home(); f.fire(); f.in.returnHome = true; f.step(.01); f.fault("Conflicting catapult controls");
        f = new Fixture(); f.home(); f.fire(); f.in.fire = false; f.step(.01); f.in.returnHome = true; f.step(.01); f.fault("Return requested during firing");
        f = new Fixture(); f.home(); f.fire(); f.in.emergencyStop = true; f.step(.01); f.fault("Emergency stop");
        f = new Fixture(); f.home(); f.fire(); f.in.started = false; f.step(.01); f.fault("OpMode no longer active");
        f = new Fixture(); f.home(); f.cycle.update(Double.NaN, f.in); f.fault("Invalid input or clock");
        f = new Fixture(); f.home(); f.cycle.update(-1, f.in); f.fault("Invalid input or clock");
        f = new Fixture(); f.home(); f.in.left = f.in.right = 5; f.in.fire = true; f.step(.01); f.fault("Fire requested away from home");
        c = fixtureConfig(); f = new Fixture(c); c.launchPower = 99; c.fire = 10000;
        f.home(); f.fire(); check(f.cycle.power() == .8 && f.cycle.target() == 100, "configuration is snapshotted");
        c = fixtureConfig(); c.minimum = -110; c.maximum = 10; c.fire = -100;
        f = new Fixture(c); f.home(); f.fire(); check(f.cycle.target() == -100 && f.cycle.power() > 0, "negative target uses positive RUN_TO_POSITION power");
        System.out.println(checks + " catapult checks passed");
    }
}
