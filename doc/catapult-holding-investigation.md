# Catapult retention and controller investigation

## Evidence from this repository

Baseline commit: `cb9775ec53fb336e41d1ccf98baa92d34766bfeb`.

- `subsystems/Catapult.java` maps `catapultRight` and `catapultLeft` as `DcMotor`, uses `RUN_WITHOUT_ENCODER`, sets both to `BRAKE`, and applies the same requested power to both. No motor model, gear ratio, direction reversal, encoder homing, travel limit, latch servo, latch sensor or current check is specified there.
- `TeleOpBindings.java` binds driver 1 R2 (>0.5) to full positive power for 500 ms, and R1 to negative power for 800 ms. Those are press-triggered timed actions, not hold-to-load/release-to-retain behavior. They are not measured load/shot limits. Interrupted timed sequences can skip their final zero-power command; characterize cancellation before reusing them for tensioning.
- No catapult CAD, load curve, gearbox identification, spring specification, latch mechanism or wiring configuration was established from the repository tree. A source declaration cannot prove the installed motors, wiring, directions, gearing, feedback or mechanism travel.

The existing TeleOp and catapult classes are **unchanged**. New logic does not retrofit a holding mode into them.

## Holding choices

| Method | Assessment |
| --- | --- |
| Zero power / BRAKE | BRAKE electrically opposes motor rotation; it is not a positive mechanical lock. Dynamic braking torque falls as speed approaches zero. Friction may resist motion, but secure retention under spring load or power loss is not established. |
| Gearbox alone | Gear type, ratio, friction, efficiency, wear and applied reverse torque are unknown. High reduction does not prove non-backdrivability. Even a worm gearbox needs manufacturer information and measured reverse-load behavior; do not assume it self-locks. |
| Encoder + RUN_TO_POSITION | May position a verified mechanism, but provides active electrical holding and can continue drawing stall-like current against a load or unreachable target. It needs homing, actual encoder counts/gearing, travel bounds, both motor feedback, synchronization monitoring, timeout and a passive safe state. It is not a substitute for a latch or a power-loss retention plan. No implementation is enabled. |
| Mechanical retention | Preferred direction for holding stored energy with little/no loading-motor current. Existing compatibility is unknown until photos/CAD, available shaft space and load measurements are supplied. |

[REV's encoder-mode documentation](https://docs.revrobotics.com/duo-control/hello-robot-blocks/part-3/using-encoder) describes continuous position holding and overheating when an unreachable position is commanded. [REV's brake/coast explanation](https://docs.revrobotics.com/duo-control/menu/adding-more-motors/sparkmini-motor-controller#zero-power-behavior) describes electrical braking. The latter explains the principle; it does not identify Morpheus's motor controller or certify its holding torque.

## Simplest candidate design to evaluate

For **release-at-any-loading-position** behavior, investigate a **spring-engaged pawl and ratchet on the tensioning shaft**, with the servo moving a release linkage rather than carrying the spring load. It can passively catch intermediate positions. A single hook/latch is potentially simpler if loading always reaches one fixed endpoint, but it cannot safely promise retention when the user releases early; that distinction must be decided mechanically.

This is a design recommendation, not a fabrication-ready design or verification of available hardware. Establish the actual load path, shaft diameter, ratchet radius/tooth spacing, pawl engagement depth and material, mounting stiffness, spring travel, maximum stored energy, side loads, interference, reverse rotation and power-loss state. Estimate tooth force from shaft torque divided by ratchet pitch radius. Select allowable stress/fatigue and an appropriate design margin with a competent mechanism reviewer; do not pick dimensions or a safety factor from this software investigation.

The latch should default to retention without electrical power and remain engaged on STOP/fault/brownout. Add independent engagement confirmation and travel/home feedback. A loaded pawl may wedge or overload a release servo: measure release force at worst load and determine whether a verified unloading/cam linkage is required. Do not command a motor to stall against the latch. A guarded manual procedure must be able to discharge or support stored energy without relying on software or hands in the travel path.

Loading must stop before overtravel. Once passive retention is positively confirmed, remove loading-motor power and verify that the mechanism remains supported. A deliberate firing command may release only with full-load, engagement and clear-state prerequisites satisfied. After discharge, verify the mechanism reaches its safe home and the passive catch resets before another load cycle. The actual motor/servo sequence remains unimplemented until the mechanism is characterized.

## Current and heating estimate: required inputs

No defensible numeric holding current or safe hold duration can be calculated from this repository. Obtain each motor's manufacturer/model, torque constant or stall torque/current, winding resistance, permitted duty/thermal limits, total gear ratio and efficiency, and measured spring torque versus arm angle. Verify whether the two motors share torque evenly.

An ideal first estimate per motor is `I_hold ≈ torque_at_arm / (N × gear_ratio × efficiency × torque_constant)`, where N=2 only if both motors demonstrably share load equally. Account separately for friction, imbalance and transmission losses. Copper heating follows approximately `P_copper = I_rms² × winding_resistance` per motor; temperature also depends on cooling and hold time. These relationships are not a safe operating limit. A latch can remove motor holding demand while still retaining significant hazardous mechanical energy.

For a future REV `DcMotorEx` adapter, verify supported current telemetry, read **both motors**, configure a measured current alert, and explicitly stop loading on overcurrent. An alert such as `isOverCurrent()` reports a condition; do not treat it as automatic load disconnection. Software checks need stale-sample/communication detection and a watchdog as well as mechanical limits. Current alone does not identify arm position or safe latching. Do not invent an amp threshold from a motor power percentage or the hub's maximum rating.

## Controller logic delivered now

`CatapultStateMachine` is pure Java with **IDLE, LOADING, HELD, FIRING, RECOVERING, FAULT**. It never writes power or maps a device. Its outputs are advisory requests for a future reviewed adapter. It requires verified hardware/sensors, fresh control edges and confirmed passive retention. A partial load can be HELD only when mechanically retained; firing additionally requires full-load confirmation.

Faults suppress loading and release requests and retain the latch request. Emergency stop, overcurrent, limits/synchronization faults, unhealthy sensors and timeouts latch FAULT. No controller button clears it. Reinitialization requires a physically safe, verified home state; it is not a way to bypass or discharge a loaded fault. A software retain request cannot physically secure an unverified latch.

`CatapultLogicPreview` is an optional, separate **NO MOTORS** OpMode:

- **Gamepad 2 left bumper:** load request while held; release requests retention through the model.
- **Gamepad 2 right trigger + A:** deliberate fire request. Trigger hysteresis is 0.7 to activate / 0.3 to release. The combination must be released while HELD and freshly pressed; holding it through loading cannot fire accidentally.
- **Either BACK:** fault/emergency stop.
- **Gamepad 1 A/B/Y/X:** synthetic home / retention / fully-loaded / shot-complete signals. These are manually injected facts, not real sensor evidence. LB/RB inject overcurrent/overtravel.

The preview maps no motors, servos, drivetrain or existing Catapult subsystem. Its 5/1/5 second timeouts are illustrative logic-test settings only. Desktop tests exercise the same state machine without a Control Hub. The existing driver 1 R1/R2 controls still behave as before in the existing TeleOp.

## Measurements still required

1. Motor/gearbox labels, complete gearing/load path, mounting orientation, hub ports and both encoder connections/resolutions/directions.
2. Arm travel, home/loaded angles, safe hard-stop locations, spring/elastic load curve and maximum torque/energy.
3. Existing retention hardware (photos/CAD), available latch mounting, intermediate-release requirement, backlash and backdrive behavior in a guarded/unloaded characterization.
4. Limit/home/engagement sensing and failure states; paired-motor synchronization limits and power-loss behavior.
5. Measured per-motor loading/holding current, release force, winding temperature/duty behavior and battery conditions under an approved guarded procedure.
6. A safe firing/reset/unloading sequence and independent emergency stop/recovery procedure.

No robot was connected, moved, tensioned or fired during this implementation. The next physical step is identification and measurements, not enabling a motor-stalling hold.
