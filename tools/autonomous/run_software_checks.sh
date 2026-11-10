#!/bin/sh
# Runs without Android, FTC hardware, Gradle or new Java dependencies once Pedro core is cached.
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
MORPHEUS_PEDRO_JAR=${PEDRO_CORE_JAR:-$(find "${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1/com.pedropathing/core/3.0.2" -name 'core-3.0.2.jar' -print -quit 2>/dev/null || true)}
if [ ! -f "$MORPHEUS_PEDRO_JAR" ]; then
    echo 'Pedro core 3.0.2 is not cached. Sync Gradle or set PEDRO_CORE_JAR to that jar.' >&2
    exit 1
fi
python3 tools/autonomous/generate_route.py --check
python3 -m unittest discover -s tools/autonomous -p 'test_*.py'
mkdir -p build/software-checks/classes
MORPHEUS_PACKAGE=TeamCode/src/main/java/org/firstinspires/ftc/teamcode
"$MORPHEUS_JAVAC" --release 8 -cp "$MORPHEUS_PEDRO_JAR" -d build/software-checks/classes \
    "$MORPHEUS_PACKAGE/autonomous/DrivePauseParkRoute.java" \
    "$MORPHEUS_PACKAGE/autonomous/DrivePauseParkSequence.java" \
    "$MORPHEUS_PACKAGE/catapult/CatapultStateMachine.java" \
    tools/autonomous/java/SoftwareChecks.java
"$MORPHEUS_JAVA" -cp "build/software-checks/classes:$MORPHEUS_PEDRO_JAR" SoftwareChecks build/software-checks/trace.csv
python3 tools/autonomous/render_trace.py
