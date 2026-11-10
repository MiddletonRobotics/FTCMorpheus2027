# Morpheus: drive, simulated shot, park

This is a software-first diagnostic, not a competition autonomous. It never maps or commands a catapult. Existing TeleOp, hardware constants, dependencies and `JustParkAuto` are unchanged.

## At home: edit and preview

1. Open [the official Pedro visualizer](https://visualizer.pedropathing.com/).
2. Use **File → Open from computer…** and select `paths/drive-pause-park.pp.json` from this checkout.
3. In Settings, select **BIOBUZZ Field (2026–2027)** if it is not already selected. Field-map choice is an editor preference, not saved in the project's `robot` block.
4. Drag the start, shooting endpoint, control point or parking endpoint. Select a path to edit its end coordinates and **Linear** or **Constant** heading. Keep headings continuous: path 1 begins at the start pose's heading, and path 2 begins at path 1's ending heading.
5. Select **Proposed shooting position**. Its **Wait after the path (ms)** is **3000**. Change that value to configure the simulated shot. Keep the second path's wait at zero. Do not add a second sequence wait: the converter rejects duplicate waits.
6. Press **Play**. The robot travels to the shooting point, pauses, then continues to the parking point. The editor estimates motion timing; it does not execute Java or verify localization/physics.
7. Use **File → Save to computer…** to overwrite the checked-out `.pp.json`. Saving only inside the browser does not update the repository.
8. From the repository root, regenerate and check:

   ```sh
   python3 tools/autonomous/generate_route.py
   sh tools/autonomous/run_software_checks.sh
   ```

9. Open `build/software-checks/preview.html` in a browser. **Play**, **Reset**, and the timeline scrubber replay the actual Java sequence trace. At **SHOOTING PAUSE**, outputs read **ZERO / STOPPED** for the configured delay. `trace.csv` contains the inspectable timestamps and states.
10. Review/commit the `.pp.json` and generated `DrivePauseParkRoute.java` together. The check command rejects stale generated Java. Do not replace the entire FTC OpMode with the visualizer's generic Java export: use this converter to preserve timeout/stop handling.

The converter accepts project version 2.0.0, exactly two atomic line/Bezier paths, constant/linear headings and one pause. It also accepts an explicit path–wait–path sequence when the path-level waits are zero. It rejects compound/through paths, unsupported headings, extra waits, duplicate paths, discontinuous headings, nonfinite/out-of-frame points and delays above 30 seconds. These limits deliberately keep the first routine small. They are not collision checking.

## What executes without hardware

The desktop command uses a JDK, Python 3 and the **real Pedro core 3.0.2 jar** from Gradle's cache. It compiles production `DrivePauseParkSequence`, production `DrivePauseParkRoute` and production `CatapultStateMachine`, executes fault-injection checks, and samples the Pedro paths. No Android SDK is required for this command once the core jar is cached.

If Java is not on PATH, the script detects Android Studio's bundled Mac JDK. Otherwise set `JAVA_HOME` to your JDK. If Pedro is not cached, sync the existing project in Android Studio first, or set `PEDRO_CORE_JAR` to the real `core-3.0.2.jar`. The script does not substitute fake Pedro classes.

The preview uses ideal progress along the path at a nominal 12 inches/second. **It does not run the FTC OpMode, Pedro's Foresight controller, REV/Pinpoint drivers, a motor/battery/contact model, or a full BIOBUZZ simulation.** Timing and coordinates are for logic/geometry inspection; actual motion is not predicted. The shown 16 × 16 inch robot is an unmeasured example.

The [Camels Hump digital twin](https://github.com/camels-hump-coders/biobuzz-digital-twin) remains a possible full-code integration experiment; SolversLib, Pedro 3 and Pinpoint compatibility was not established by this change. The [NCSSM BIOBUZZ simulator](https://biobuzz.ncssmftc.org/sim/) is a strategy resource, not an established executor of this Java routine. Neither is required to run these desktop checks.

## Coordinates and the BIOBUZZ field

Verified orientation: the [FIRST Event Field Setup Guide](https://ftc-resources.firstinspires.org/ftc/field/eventfieldguide), page 8, shows the audience at the bottom, columns A→F left-to-right and rows 1→6 bottom-to-top. Page 14 locates the red loading zone on A5 and blue on F2. This matches the orientation of the visualizer's BIOBUZZ image.

Use **inches**, origin at the **inside audience-left corner**, **+x right**, **+y away from the audience**, heading **0° toward +x**, **90° toward +y**, positive counterclockwise. The path pose denotes the robot/localizer reference point, whose physical offset still needs measurement. Headings in the JSON and generated `PoseFactory.degrees()` calls are degrees; Pedro's returned pose headings are radians. This is a Pedro frame, not the centered FTC SDK frame.

Scale caveat: the [current FIRST manual](https://ftc-resources.firstinspires.org/ftc/game) describes an approximately 144-inch square and identifies CAD as authoritative. The [official visualizer source](https://github.com/Pedro-Pathing/Visualizer/blob/97104e027af00d49b8bd61e29bcb74a9d7e62b90/src/config/defaults.ts) uses **141.5 inches of inside floor**, cropping 1.25 inches from each edge of its six nominal 24-inch tile image. This route and importer use **the editor's 0–141.5 frame**, without silently rescaling it to 144. The origin/orientation is checked against FIRST's field view; actual wall spacing, tile seams, zones and localizer origin must be measured before physical use. No exact CAD extraction or field survey was performed for this change.

| Pose | Demo x, y, heading | Meaning |
| --- | --- | --- |
| Start | 12, 84, 90° | Configurable software start; not a verified legal competition start |
| Shooting | 24, 106, 0° | Proposed pause location, not ballistically calibrated |
| Parking | 12, 106, 180° | Near the red A5 loading-zone side; overlap/legality depends on measured footprint and field |

The curve uses control point (14, 100). These are **proposed demonstration targets**, not official scoring coordinates. Validate the full swept footprint (especially the 180° turn), wall clearance, field elements, current rules and actual starting placement before enabling hardware. The trace viewer's schematic omits many collision surfaces and heights. No alliance mirroring is assumed.

## Robot implementation and stop behavior

`DrivePauseParkAuto` is shipped **@Disabled**. It constructs the existing configured follower without changing shared constants, applies a chassis-demand cap that bounds mecanum wheel power to 0.25, and starts at the route's pose. It uses its own follower, with exactly one update per active loop; it does not construct `Drivetrain` or run a second scheduler.

The sequence is `TO_SHOOT → SHOOTING_PAUSE → TO_PARK → DONE`. Both paths have an independent 10-second deadline including settling. A path must be not-busy, within 1 inch and 3° of its endpoint, and below 0.5 inches/second and 3°/second for 0.25 seconds. These are initial diagnostic thresholds, not validated accuracy specifications. Pedro endpoint timeout completion alone does not count as arrival.

At the pause, both follower state and motor outputs are stopped; the pose is monitored for drift. The full configurable pause begins after measured settling. During the pause there is no active drivetrain hold controller. Drift faults rather than continuing. Stop is invoked on completion, path timeout, failed endpoint checks, nonfinite localization, drive exceptions, gamepad 1 BACK or Driver Station STOP. Cancellation cannot restart the sequence. Motor stop writes are attempted for every mapped drive motor even if another write fails.

Pedro 3.0.2 `Follower.stop()` changes follower state; immediate motor zeroing requires `follower.drivetrain.stop(true)`/direct motor writes too. This adapter does both, including partial initialization cleanup. Communication or mechanical failure can still defeat software commands; Driver Station STOP remains the operator stop method.

Before any authorized physical run, identify the real localizer (the existing code assumes `pinpoint`), replace placeholder calibration with measured/tuned values, verify all four configured names and directions (`frontLeft`, `backLeft`, `rightFront`, `backRight`), confirm the reference pose and field clearances, and validate the stop method with wheels safely raised. Only then remove `@Disabled`, sync/build, and perform an explicitly authorized low-power test in a clear area. This change did not deploy to or move a robot.

## Android Studio / Git

For a fresh checkout, use this PR's feature branch `codex/drive-pause-park`; fetch it in Android Studio and check it out without discarding local changes. For an existing checkout:

```sh
git status
git fetch origin
git switch --track origin/codex/drive-pause-park
```

If that local branch already exists, switch to it and pull with `--ff-only`. Review uncommitted work before switching. Sync Gradle and run `sh gradlew :TeamCode:assembleDebug`. Configure Android Studio's Gradle JDK if needed. None of these steps deploys the application; deployment/hardware testing is a separate authorized action.
