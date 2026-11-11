package org.firstinspires.ftc.teamcode.catapult;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/** Controller logic exercise: never constructs a hardware device or the existing Catapult subsystem. */
@TeleOp(name = "Catapult Logic Preview - NO MOTORS", group = "Simulation")
public final class CatapultLogicPreview extends OpMode {
    // Preview timings only. No real mechanism parameters are implied.
    private CatapultStateMachine model;
    private boolean triggerActive;

    @Override public void init() { model = new CatapultStateMachine(5, 1, 5); }
    @Override public void loop() {
        if (gamepad2.right_trigger > 0.7) triggerActive = true;
        else if (gamepad2.right_trigger < 0.3) triggerActive = false;
        CatapultStateMachine.Inputs in = new CatapultStateMachine.Inputs();
        in.loadHeld = gamepad2.left_bumper;
        in.firePressed = triggerActive && gamepad2.a; // Deliberate two-control fire command.
        in.emergencyStop = gamepad1.back || gamepad2.back;
        in.hardwareVerified = true; // SYNTHETIC, not evidence about the robot.
        in.sensorsHealthy = true;
        in.home = gamepad1.a;
        in.retentionConfirmed = gamepad1.b;
        in.fullyLoaded = gamepad1.y;
        in.shotComplete = gamepad1.x;
        in.overCurrent = gamepad1.left_bumper;
        in.limitExceeded = gamepad1.right_bumper;
        in.holdingMechanismFault = gamepad1.dpad_down;
        model.update(System.nanoTime() / 1e9, in);
        telemetry.addLine("LOGIC ONLY: no motors or latch servo are mapped.");
        telemetry.addData("Gamepad 2", "LB load; RT + A deliberate fire; BACK fault");
        telemetry.addData("Synthetic gamepad 1 sensors", "A home, B retention, Y loaded, X shot complete");
        telemetry.addData("Fault injection", "gamepad 1 LB current, RB overtravel; BACK E-stop");
        telemetry.addData("Retaining spring failure", "gamepad 1 DPAD DOWN");
        telemetry.addData("State", model.state());
        telemetry.addData("Fault", model.fault());
        telemetry.addData("Advisory requests", "load=%s release=%s retain=%s",
                model.loadRequested(), model.releaseRequested(), model.retainRequested());
        telemetry.update();
    }
    @Override public void stop() {
        if (model != null) {
            CatapultStateMachine.Inputs in = new CatapultStateMachine.Inputs();
            in.emergencyStop = true;
            model.update(System.nanoTime() / 1e9, in);
        }
    }
}
