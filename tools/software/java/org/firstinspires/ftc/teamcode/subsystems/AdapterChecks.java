package org.firstinspires.ftc.teamcode.subsystems;

import java.lang.reflect.*;
import java.util.*;
import com.qualcomm.robotcore.hardware.*;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.catapult.CatapultCycle;
import org.firstinspires.ftc.teamcode.catapult.CatapultSettings;
import org.firstinspires.ftc.teamcode.commands.DrivetrainController;

public final class AdapterChecks {
    static int checks;
    static void check(boolean yes, String label) { checks++; if (!yes) throw new AssertionError(label); }
    static class Motor implements InvocationHandler {
        int position = 47, target, resets, positiveWrites;
        double power, current;
        boolean failRead, failZeroOnce;
        DcMotor.RunMode mode;
        final DcMotorEx device = (DcMotorEx) Proxy.newProxyInstance(DcMotorEx.class.getClassLoader(), new Class[]{DcMotorEx.class}, this);
        public Object invoke(Object proxy, Method m, Object[] a) {
            switch (m.getName()) {
                case "setPower":
                    if ((Double)a[0] == 0 && failZeroOnce) { failZeroOnce = false; throw new IllegalStateException("test write failure"); }
                    power = (Double)a[0]; if (power != 0) positiveWrites++; return null;
                case "getPower": return power;
                case "getCurrentPosition": if (failRead) throw new IllegalStateException("test read failure"); return position;
                case "getCurrent": return current;
                case "getVelocity": return 0.;
                case "setTargetPosition": target = (Integer)a[0]; return null;
                case "getTargetPosition": return target;
                case "setMode": mode = (DcMotor.RunMode)a[0]; if (mode == DcMotor.RunMode.STOP_AND_RESET_ENCODER) { position = 0; resets++; } return null;
                case "getMode": return mode;
                case "hashCode": return System.identityHashCode(proxy);
                case "equals": return proxy == a[0];
                case "toString": return "Simulated motor registers";
            }
            if (m.getReturnType() == boolean.class) return false;
            if (m.getReturnType() == int.class) return 0;
            if (m.getReturnType() == double.class) return 0.;
            return null;
        }
    }
    static CatapultCycle.Config config() {
        CatapultCycle.Config c = new CatapultCycle.Config();
        c.enabled = c.safeAtZeroPowerVerified = true;
        c.home = 0; c.fire = 100; c.minimum = -10; c.maximum = 110;
        c.tolerance = 3; c.maximumSkew = 15; c.minimumProgress = 2;
        c.launchPower = .8; c.returnPower = .3; c.maximumCurrent = 3;
        c.fireTimeout = 2; c.returnTimeout = 3; c.progressTimeout = .4; c.settleTime = .1;
        return c;
    }
    static class Rig {
        final Motor left = new Motor(), right = new Motor();
        final Catapult catapult;
        boolean started, fire, back, confirm;
        double now;
        Rig(CatapultCycle.Config c) {
            HardwareMap map = new HardwareMap(); map.put("catapultLeft", left.device); map.put("catapultRight", right.device);
            catapult = new Catapult(map, c, () -> now);
            catapult.bindControls(() -> fire, () -> false, () -> back, () -> confirm, () -> started);
        }
        void step(double dt) { now += dt; catapult.periodic(); }
        void launch() { step(0); confirm = true; step(.01); confirm = false; started = true; step(.01); fire = true; step(.01); }
    }
    public static void main(String[] args) {
        Rig r = new Rig(CatapultSettings.measuredConfiguration());
        check(r.left.resets == 0 && r.right.resets == 0 && r.left.position == 47, "constructor preserves encoder references");
        r.launch(); check(r.left.positiveWrites == 0 && r.right.positiveWrites == 0, "production config never commands motion or reset");
        check(r.left.resets == 0 && r.right.resets == 0, "disabled home confirmation cannot reset encoders");
        Set<String> keys = new HashSet<>();
        Telemetry telemetry = (Telemetry) Proxy.newProxyInstance(Telemetry.class.getClassLoader(), new Class[]{Telemetry.class}, (proxy, method, values) -> {
            if (method.getName().equals("addData")) keys.add((String)values[0]);
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == int.class) return 0;
            return null;
        });
        r.catapult.addTelemetry(telemetry);
        check(keys.contains("Catapult encoder L/R") && keys.contains("Catapult current A L/R")
                && keys.contains("Catapult velocity ticks/s L/R") && keys.contains("Catapult status"), "telemetry exposes paired feedback and reason");
        r = new Rig(config()); r.launch();
        check(r.left.resets == 1 && r.right.resets == 1, "explicit INIT confirmation resets each encoder once");
        check(r.left.target == 100 && r.right.target == 100 && r.left.power == .8 && r.right.power == .8, "adapter applies cycle target and power");
        r.back = true; r.step(.01); check(r.left.power == 0 && r.right.power == 0, "BACK zeroes both motors");
        r.back = false; r.step(.01); check(r.left.power == 0 && r.right.power == 0, "BACK fault stays latched");
        r = new Rig(config()); r.launch(); r.right.failRead = true; r.step(.01);
        check(r.left.power == 0 && r.right.power == 0, "feedback exception zeroes both motors");
        r = new Rig(config()); r.launch(); r.step(.41);
        check(r.left.power == 0 && r.right.power == 0 && r.left.target == 100, "stall faults without issuing home target");
        r = new Rig(config()); r.launch(); r.catapult.stop();
        check(r.left.power == 0 && r.right.power == 0, "OpMode end zeroes both motors");
        r.step(.01); check(r.left.power == 0 && r.right.power == 0, "stop cannot restart");
        r = new Rig(config()); r.launch(); r.left.failZeroOnce = true;
        try { r.catapult.stop(); } catch (IllegalStateException expected) { }
        check(r.right.power == 0, "right stop still attempted when left write fails");
        r.step(.01); check(r.left.power == 0, "latched stop remains zero after recoverable write failure");
        Drivetrain drivetrain = new Drivetrain();
        new DrivetrainController(drivetrain, () -> .7, () -> .2, () -> -.4).execute();
        check(drivetrain.forward == .7, "forward reaches forward argument");
        check(drivetrain.strafe == -.4, "strafe reaches strafe argument");
        check(drivetrain.rotation == .2, "rotation remains unchanged");
        System.out.println(checks + " adapter/drive checks passed (simulated devices; real production adapter and controller)");
    }
}
