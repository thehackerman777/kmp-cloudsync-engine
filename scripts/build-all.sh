#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "=== Building all CloudSync Engine artifacts ==="
cd "$PROJECT_DIR"

echo ""
echo "--- Full Gradle build (all modules) ---"
./gradlew build --no-daemon -q

echo ""
echo "--- Engine platform artifacts ---"
./gradlew :engine:buildAllArtifacts :engine:publishToMavenLocal --no-daemon -q

echo ""
echo "--- Artifact sizes ---"
echo ""
echo "Desktop JAR:"
find engine/build/libs -name "*.jar" -exec ls -lh {} \;
echo ""
echo "Android AAR:"
find engine/build/outputs/aar -name "*.aar" -exec ls -lh {} \;
echo ""
echo "JS Bundle:"
find engine/build/outputs/js -name "*.js" -exec ls -lh {} \;
echo ""
echo "MavenLocal artifacts:"
find "$HOME/.m2/repository/io/cloudsync" -type f 2>/dev/null | sort || echo "(none yet)"

echo ""
echo "=== All artifacts built successfully! ==="
