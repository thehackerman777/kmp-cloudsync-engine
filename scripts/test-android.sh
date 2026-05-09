#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_SDK="${ANDROID_HOME:-/home/ubuntu/android-sdk}"

echo "=== Building Android sample app ==="
cd "$PROJECT_DIR"

echo ""
echo "--- Ensure engine is published to MavenLocal ---"
./gradlew :engine:publishToMavenLocal --no-daemon -q

echo ""
echo "--- Build Android sample (compileDebugKotlin) ---"
cd samples/android-app
if [ ! -f "build.gradle.kts" ]; then
    echo "ERROR: samples/android-app/build.gradle.kts not found!"
    exit 1
fi

# Create a minimal settings.gradle.kts for the sample
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

rootProject.name = "cloudsync-android-sample"
EOF

# Create local.properties
echo "sdk.dir=$ANDROID_SDK" > local.properties

# Copy gradle wrapper if needed
if [ ! -f "gradlew" ]; then
    cp "$PROJECT_DIR/gradlew" .
    cp -r "$PROJECT_DIR/gradle" .
fi

./gradlew compileDebugKotlin --no-daemon -q
echo ""
echo "Android sample: compileDebugKotlin SUCCEEDED!"

# Clean up
rm -f settings.gradle.kts local.properties
rm -rf gradle gradlew gradlew.bat

cd "$PROJECT_DIR"
echo ""
echo "=== Android sample build completed! ==="
