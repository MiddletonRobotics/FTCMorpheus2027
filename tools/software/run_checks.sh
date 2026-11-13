#!/bin/sh
# Desktop logic + SDK-interface checks. No APK deployment or physical hardware access.
set -eu
cd "$(dirname "$0")/../.."
if [ -n "${JAVA_HOME:-}" ]; then
    MORPHEUS_JAVA="$JAVA_HOME/bin/java"
    MORPHEUS_JAVAC="$JAVA_HOME/bin/javac"
elif [ -x '/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/javac' ]; then
    MORPHEUS_JAVA='/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java'
    MORPHEUS_JAVAC='/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/javac'
else
    MORPHEUS_JAVA=java
    MORPHEUS_JAVAC=javac
fi
mkdir -p build/software-checks/classes build/software-checks/lib
python3 - <<'PY'
import os
from pathlib import Path
from zipfile import ZipFile
cache = Path(os.environ.get('GRADLE_USER_HOME', str(Path.home()/'.gradle'))) / 'caches/modules-2/files-2.1'
for group, artifact, version in [('org.solverslib','core','0.3.6'), ('org.firstinspires.ftc','RobotCore','12.0.0')]:
    candidates = list((cache/group/artifact/version).glob('*/*.aar'))
    if not candidates:
        raise SystemExit('Sync Gradle first: missing cached ' + group + ':' + artifact + ':' + version)
    with ZipFile(candidates[0]) as archive:
        Path('build/software-checks/lib', artifact+'.jar').write_bytes(archive.read('classes.jar'))
PY
MORPHEUS_PACKAGE=TeamCode/src/main/java/org/firstinspires/ftc/teamcode
MORPHEUS_CP=build/software-checks/lib/core.jar:build/software-checks/lib/RobotCore.jar
"$MORPHEUS_JAVAC" --release 8 -proc:none -cp "$MORPHEUS_CP" -d build/software-checks/classes \
    "$MORPHEUS_PACKAGE/catapult/CatapultCycle.java" \
    "$MORPHEUS_PACKAGE/catapult/CatapultSettings.java" \
    "$MORPHEUS_PACKAGE/subsystems/Catapult.java" \
    "$MORPHEUS_PACKAGE/commands/DrivetrainController.java" \
    tools/software/stubs/com/qualcomm/robotcore/hardware/HardwareMap.java \
    tools/software/stubs/org/firstinspires/ftc/teamcode/subsystems/Drivetrain.java \
    tools/software/java/CatapultCycleChecks.java \
    tools/software/java/org/firstinspires/ftc/teamcode/subsystems/AdapterChecks.java
"$MORPHEUS_JAVA" -cp "build/software-checks/classes:$MORPHEUS_CP" CatapultCycleChecks
"$MORPHEUS_JAVA" -cp "build/software-checks/classes:$MORPHEUS_CP" org.firstinspires.ftc.teamcode.subsystems.AdapterChecks
