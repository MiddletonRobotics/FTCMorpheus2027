# Test results — November 10, 2026

Base: `cb9775ec53fb336e41d1ccf98baa92d34766bfeb`. Feature branch: `codex/drive-pause-park`. PR: https://github.com/MiddletonRobotics/FTCMorpheus2027/pull/1.

| Check | Result |
| --- | --- |
| Baseline `sh gradlew :TeamCode:assembleDebug` | PASS |
| Baseline `:TeamCode:dependencies --configuration debugRuntimeClasspath` | PASS: Pedro core/revhub 3.0.2, tuning 1.0.2 (shared 1.0.1 declaration resolves upward) |
| Final `sh gradlew :TeamCode:assembleDebug` | PASS |
| `sh tools/autonomous/run_software_checks.sh` | PASS: 143 Java assertions and 4 Python tests |
| Generated route matches saved visualizer JSON | PASS |
| Java trace shooting pause | Begins at 2.440 s, park begins at 5.440 s: 3.000 s; output requests stopped throughout |
| Java trace termination | DONE; outputs stopped |
| Preview JavaScript syntax (`node --check`) | PASS |
| `git diff --check` | PASS |
| Official visualizer route import and wait edit/save | Performed through browser/native UI. Route geometry imported; 3000 ms path wait saved. A duplicate sequence wait was removed from the saved project; the converter rejects duplicates. Final desktop checks ran on the corrected project. |
| Physical robot, catapult loading/firing, hardware deployment | NOT RUN |
| Full FTC OpMode/Foresight/Pinpoint execution in a physics simulator | NOT RUN |

Build environment: Android Studio bundled OpenJDK 25.0.3, Gradle 9.1.0, AGP 8.13.2, FTC SDK artifacts 12.0.0. The build installed missing Android API 34 under the existing accepted SDK license. No dependency version changes were required. Existing Java 8/source-target, annotation-processor and Gradle deprecation warnings remain; the final build has no compilation errors.

Checks exercise startup and path continuity, full pause duration, zero output requests during the pause, first/second path timeouts, false endpoint completion, moving/drifting conditions, cancellation in every movement phase, invalid localization/clock and drive exceptions. Catapult checks include normal load/retain/fire/recovery, startup-held controls, fresh fire interlocks, partial loads, lost retention, loading/firing/recovery timeouts, latched faults and stop/current/limit/sensor/verification faults in every active state. The importer checks visual edits/delay propagation and rejects unsupported or inconsistent route behavior.

The desktop motion source is ideal synthetic progress along the actual Pedro path geometry. These results validate sequence/control logic and build compatibility, not robot tracking accuracy, motor torque/current, secure mechanical retention, competition start legality, field clearance or shot performance. See the [home workflow](drive-pause-park.md) and [hardware investigation](catapult-holding-investigation.md) before any separately authorized hardware test.
