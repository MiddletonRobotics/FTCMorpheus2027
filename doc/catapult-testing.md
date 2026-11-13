# Catapult checks before hardware testing

This branch builds on `56d1e80` (the new bot push). Catapult motor output is **disabled by default**. The desktop work cannot establish motor directions, encoder wiring, travel, current limits, PID gains, or how the actual arm stays down. Drive and intake remain available with the existing configured devices.

## Changes

- Corrected the forward/strafe argument swap in `DrivetrainController`.
- Replaced the instant fire/return requests with a continuously supervised `CatapultCycle` owned by the catapult subsystem.
- A firing timeout faults instead of starting a return. Return has its own deadline and must finish before another shot is accepted.
- Both encoders must reach and remain within the target tolerance for the configured settle time. Travel, paired-position disagreement, progress timeout, and per-motor current are monitored while enabled. Progress is checked independently for each motor.
- Faults latch and request zero output. BACK on either controller and TeleOp exit also stop both motors. Zeroing the right motor is attempted even if the left write throws.
- Initialization preserves encoder counts. An explicit manual home-reference confirmation is accepted only during INIT after the confirmation controls have been released. Starting without confirmation blocks motion.
- Both TeleOps show state, fault/disabled reason, left/right positions, errors, target, currents, velocities, feedback availability, and requested output during INIT and operation.

Zero motor output and BRAKE do not provide a mechanical lock. This controller does not implement a continuously powered holding strategy. A known failed retaining spring must be resolved, or another verified means must make zero output acceptable, before enabling movement. A latched software fault cannot physically support an arm.

## Run without a robot

From the repository root:

```sh
sh tools/software/run_checks.sh
```

The script uses Java, Python 3, and the SolversLib 0.3.6 / FTC RobotCore 12.0.0 AARs cached by Gradle. Sync the Android project first if they are missing. It does not connect to or deploy to a Control Hub.

The checks run the production controller, motor adapter, and drive command. A substitute HardwareMap and simulated SDK motor interfaces record requests; the actual SolversLib command library is used. Tests cover normal completion, unhomed/held-start controls, fresh home confirmation, independent motor stalls, skew, travel, timeouts, invalid feedback/clock/configuration, emergency stop, cancellation, retry prevention, and the corrected drive arguments. The fixture's encoder/current/time/power numbers are illustrative test data and must not be copied into the robot settings.

To compile the Android application separately:

```sh
sh gradlew :TeamCode:assembleDebug --offline --no-daemon
```

Use a JDK supported by the installed Android/Gradle toolchain. Validation here used Android Studio's bundled JDK. The build produces an APK; it does not install it.

These checks establish software behavior, not actual motor torque, PID stability, firing speed, mechanical retention, or shot accuracy.

## Hardware facts needed before enabling

Fill `CatapultSettings.measuredConfiguration()` only after establishing all of the following:

| Configuration | Evidence needed |
| --- | --- |
| `enabled` | Leave false until the configuration and a guarded test plan have been reviewed. |
| `safeAtZeroPowerVerified` | Verified physical behavior on zero output, STOP, faults, and loss of power. Repair failed retention where needed. |
| `home` | Must remain 0. Confirm the arm's repeatable physical home before zeroing. |
| `fire`, `minimum`, `maximum` | Measured encoder travel after home; leave clearance before damaging mechanical stops. |
| `tolerance`, `maximumSkew` | Measured positioning accuracy, backlash, and allowable disagreement between the two sides. |
| `minimumProgress`, `progressTimeout` | Expected per-motor progress at the slowest approved speed/load, accounting for loop timing. |
| `launchPower`, `returnPower` | Verified movement settings, not guesses or continuous holding limits. |
| `maximumCurrent` | A measured per-motor operating threshold consistent with motor, hub, wiring, and mechanism limits. Software polling is not an independent electrical cutoff. |
| `fireTimeout`, `returnTimeout`, `settleTime` | Measured worst-case movement/settling times under approved loads. |

Both motor encoders must be connected, correctly assigned to their motor ports, and report consistent direction and units for the shared arm. The right-motor reversal comes from the new push and remains physically unverified. Current telemetry must work for both motors. Disconnected encoders can return plausible zero readings; the progress watchdog only detects that after an enabled movement request, so wiring verification must come first.

The code still uses the hub's built-in RUN_TO_POSITION controller. No PID gains were invented. Once mechanics, references, and limits are established, capture both positions/errors, velocities, currents, and response timing to tune position P and velocity PIDF as needed. Position repeatability alone does not establish repeatable launch velocity.

## Controls after hardware verification

| Control | Behavior |
| --- | --- |
| Gamepad 2 X + Y, during INIT only | After physically establishing home, release the combination and press it again to confirm the reference and reset both encoders. It is an operator assertion, not a sensor measurement. Disabled settings reject it. |
| Driver 1 R2 > 0.5 | Fresh press requests one shot from confirmed home, then a supervised return. |
| Driver 1 R1 | Fresh press requests return from a homed idle state. It cannot blindly home an unreferenced arm. Requesting return during firing faults instead of abruptly reversing. |
| Either BACK | Latches catapult fault and requests zero motor output. It is not a whole-robot emergency stop; Driver Station STOP remains the whole-robot stop. |

Release R1 and R2 between cycles. A fresh R2 press during return is ignored and cannot become a delayed shot. A firing timeout, return timeout, sensor fault, overcurrent, skew/travel fault, cancellation, or BACK requires a physically safe inspection and OpMode reinitialization; no button clears it. Reinitialization does not discharge or secure a loaded mechanism.

## Validation

- 122 controller assertions and 17 adapter/drive assertions pass (139 total).
- `:TeamCode:assembleDebug` passes against the project's actual dependencies.
- No APK deployment, physical movement, catapult loading/firing, PID tuning, or retention test was performed.

The earlier drive/pause/park autonomous remains on `codex/drive-pause-park` / PR #1. This branch starts from the new main code and does not merge the two implementations. Their overlapping catapult changes need reconciliation before combining them.
