# Catapult holding follow-up

The user reports that the spring retaining the catapult down is failing. This is a known retention fault, not evidence that a motor can safely replace it. The new `holdingMechanismFault` input latches FAULT from every state and suppresses loading/release requests. A future hardware adapter must provide the real fault signal; the logic-only preview injects it with gamepad 1 DPAD DOWN. This does not modify or protect the existing TeleOp's direct motor commands.

## What was found

- The local `26MBB-000.stp` CAD file, exported October 21, 2026, includes catapult assembly `26MBB-300- Catapult V2`. Its assembly usage records #1488 and #1508 place two components labelled `60RPM 133.2 kg.cm` inside the catapult subassembly (#1573677). Records #1482/#1502 name two 60T hub-mount gears; #1490/#1491/#1510/#1511 name four 30T miter gears. These component names do not establish the full mechanical reduction, load path, spring state, or actual installed parts.
- Those motor labels match multiple goBILDA 99.5:1 planetary models. For example, the [5203-2402-0100 manufacturer sheet](https://www.gobilda.com/content/spec_sheets/5203-2402-0100_spec_sheet.pdf) lists 60 RPM, 133.2 kg·cm stall torque, 9.2 A stall current at 12 V and 2786.2 encoder counts per gearbox-output revolution. The [5202-2402-0100](https://www.gobilda.com/5202-series-yellow-jacket-planetary-gear-motor-99-5-1-ratio-24mm-length-6mm-d-shaft-60-rpm-36mm-gearbox-3-3-5v-encoder/) shares these numbers. Exact model and encoder wiring remain unverified. **9.2 A is a stall specification, not a permitted continuous holding current or a configured current cutoff.**
- No connected devices were reported by `adb devices -l`. No robot configuration, current, encoder motion, retaining load or safe travel was measured.
- Android Studio's existing `~/StudioProjects/FTCMorpheus2027` checkout is at an older commit and has user edits. It was not used for implementation or reset. The feature checkout remains in the Morpheus Robotics folder.

## Code provided

`CatapultHardwareCheck` is a separate, disabled **READ ONLY** diagnostic. It maps only `catapultLeft` and `catapultRight` and reports software-configured motor type, ticks/rev, gearing, connection/port, mode, direction, zero-power behavior, encoder position/change/range, existing requested power, velocity and current where supported. It does not call any motor setter, reset an encoder, load a mechanism, command a hold or release a latch. It does not construct the existing Catapult subsystem, whose constructor changes motor modes.

It remains `@Disabled`. With the failed spring, mechanically support and unload the catapult independently before a separately authorized diagnostic session. The diagnostic cannot secure it. Verify the actual labels/wiring and repeatable unloaded encoder response; a configured motor name or static count is insufficient evidence. Then establish a real home reference and safe limits, paired-motor direction/synchronization, and load/current/thermal limits before selecting a holding controller. Do not backdrive the loaded mechanism by hand to obtain these readings.

## Why an active hold is not enabled

An encoder-based hold captures position on button release and actively supplies torque to correct movement. It may require nonzero current continuously, loses torque when disabled/unpowered, and can stall/overheat against an unreachable target. [REV documents these RUN_TO_POSITION behaviors](https://docs.revrobotics.com/duo-control/hello-robot-java/part-3/using-encoder). No power percentage, current threshold, loaded endpoint, homing operation or release direction has been guessed here.

Until independent retention and those measurements are verified, there is no evidence-backed way to promise secure holding with this failed spring. Repairing the retaining spring or adding a positive catch addresses the power-loss case. A powered position hold can be evaluated afterward; it must not be represented as a zero-current mechanical lock.

## Verification

`sh tools/autonomous/run_software_checks.sh` passes 162 Java assertions and 4 Python tests, including the known-retention-fault input in IDLE, LOADING, HELD, FIRING and RECOVERING. `:TeamCode:assembleDebug` passes against the project's real FTC/Pedro dependencies. Source inspection confirms the hardware check contains no motor setter calls. No APK was deployed, no diagnostic was run on hardware, and no holding current or position-control performance was measured.
