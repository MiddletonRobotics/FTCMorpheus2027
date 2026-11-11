# Catapult controls

In BlueTeleOp and RedTeleOp, driver 1 **R1/right bumper** now requests the existing down/reset direction **while held**, for at most **800 ms per press**. Releasing R1 stops output immediately. At the timeout, output stays zero until both R1 and R2 have been released. It will not continuously retrigger against an endpoint.

The down command retains the previous reset polarity and power (`-1.0`) and its previous 800 ms maximum; these came from the existing code, not new motor/load measurements. Physical direction, travel and suitability still need verification. The change does not establish a safe current or temperature limit and was not deployed or physically tested.

The deadline is enforced by the running scheduler, not an independent hardware watchdog. If the control loop freezes or communication fails, the code cannot guarantee a stop at exactly 800 ms.

Driver 1 **R2/right trigger (>0.5)** retains the existing one-press, positive-power, **500 ms** launch. Both controls together stop the catapult and require both to be released; releasing R1 while holding R2 cannot turn lowering into an accidental shot. All controls must first be released after START. **BACK** latches catapult output off for the rest of the OpMode. Driver Station STOP also ends it.

`CatapultManualCommand` owns the catapult subsystem for both actions. Interruption and exceptions stop output. BlueTeleOp/RedTeleOp explicitly call `catapult.stop()` in `end()`, because this SolversLib version's scheduler `reset()` drops its instance rather than cancelling commands. Both motor stop writes are attempted even if one fails. The drivetrain and intake control maps are unchanged.

**This is bounded downward movement, not a secure loaded-position hold.** Release/timeout/STOP remove motor power. With the retaining spring failed, support/unload the mechanism independently before any test: neither BRAKE nor these commands guarantees retention. No unlimited high-power stall hold or unverified encoder target is implemented. The separate `CatapultStateMachine`/logic preview still models retention faults; it is not wired into this manual command and does not add a physical latch sensor or current cutoff.

Before any separately authorized hardware run, verify the actual motor wiring/direction, encoders, safe travel, load/current and a passive retention or controlled-unloading method. See [the hardware findings](catapult-check.md). No robot was commanded during development.

Run `sh tools/autonomous/run_software_checks.sh` for production-controller tests covering release, timeout, startup-held controls, simultaneous inputs, fresh-press rearming, preventing unintended shots/reversals, cancellation, bad clocks and emergency stop. The Android build verifies the actual SolversLib/FTC integration; software tests do not verify physical down direction or secure holding.

Verification for this change: Android `:TeamCode:assembleDebug` passed; 42 manual-controller checks, the existing 162 Java checks and 4 Python tests passed. `git diff --check` passed. No deployment or physical motion occurred.
