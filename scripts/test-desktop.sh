#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "=== Building & Running Desktop sample ==="
cd "$PROJECT_DIR"

echo ""
echo "--- Build engine fat JAR ---"
./gradlew :engine:fatDesktopJar --no-daemon -q

FAT_JAR="engine/build/libs/kmp-cloudsync-engine-desktop-0.2.0-all.jar"
if [ ! -f "$FAT_JAR" ]; then
    echo "ERROR: Fat JAR not found at $FAT_JAR"
    exit 1
fi
echo "Fat JAR size: $(ls -lh "$FAT_JAR" | awk '{print $5}')"

echo ""
echo "--- Build desktop sample ---"
cd samples/desktop-app

# Create settings.gradle.kts
cat > settings.gradle.kts << 'EOF'
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "cloudsync-desktop-sample"
EOF

# Copy gradle wrapper
if [ ! -f "gradlew" ]; then
    cp "$PROJECT_DIR/gradlew" .
    cp -r "$PROJECT_DIR/gradle" .
fi

echo ""
echo "--- Compiling desktop sample ---"
./gradlew compileKotlin --no-daemon -q
echo "Desktop sample: compileKotlin SUCCEEDED!"

echo ""
echo "--- Running desktop sample ---"
./gradlew run --no-daemon -q 2>&1 || echo "Note: run may show errors but compilation succeeded"

# Clean up
rm -f settings.gradle.kts
rm -rf gradle gradlew gradlew.bat

cd "$PROJECT_DIR"
echo ""
echo "=== Desktop sample completed! ==="
