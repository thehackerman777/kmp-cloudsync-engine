#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ERRORS=0

echo "=== Validating CloudSync Engine Artifacts ==="
echo ""

# 1. Desktop Fat JAR
echo "--- Desktop Fat JAR ---"
DESKTOP_JAR="$PROJECT_DIR/engine/build/libs/kmp-cloudsync-engine-desktop-0.2.0-all.jar"
if [ -f "$DESKTOP_JAR" ]; then
    SIZE=$(stat -c%s "$DESKTOP_JAR" 2>/dev/null || stat -f%z "$DESKTOP_JAR" 2>/dev/null || echo "0")
    SIZE_KB=$((SIZE / 1024))
    echo "  File: $DESKTOP_JAR"
    echo "  Size: ${SIZE_KB}KB"
    if [ "$SIZE" -lt 1000 ]; then
        echo "  WARNING: Desktop JAR seems too small (< 1KB)"
    else
        echo "  OK: JAR size looks reasonable"
    fi
else
    echo "  MISSING: Desktop fat JAR not found!"
    ERRORS=$((ERRORS + 1))
fi

# 2. Android AAR
echo ""
echo "--- Android AAR ---"
AAR_RELEASE="$PROJECT_DIR/engine/build/outputs/aar/engine-release.aar"
AAR_FAT="$PROJECT_DIR/engine/build/outputs/aar/engine-fat-release.aar"
if [ -f "$AAR_RELEASE" ]; then
    SIZE=$(stat -c%s "$AAR_RELEASE" 2>/dev/null || stat -f%z "$AAR_RELEASE" 2>/dev/null || echo "0")
    echo "  Release AAR: $AAR_RELEASE ($((SIZE / 1024))KB)"
else
    echo "  MISSING: Release AAR not found!"
    ERRORS=$((ERRORS + 1))
fi
if [ -f "$AAR_FAT" ]; then
    SIZE=$(stat -c%s "$AAR_FAT" 2>/dev/null || stat -f%z "$AAR_FAT" 2>/dev/null || echo "0")
    echo "  Fat AAR: $AAR_FAT ($((SIZE / 1024))KB)"
fi

# 3. JS Bundle
echo ""
echo "--- JS Bundle ---"
JS_BUNDLE="$PROJECT_DIR/engine/build/outputs/js/engine.js"
if [ -f "$JS_BUNDLE" ]; then
    SIZE=$(stat -c%s "$JS_BUNDLE" 2>/dev/null || stat -f%z "$JS_BUNDLE" 2>/dev/null || echo "0")
    SIZE_KB=$((SIZE / 1024))
    echo "  File: $JS_BUNDLE"
    echo "  Size: ${SIZE_KB}KB"
    if [ "$SIZE" -lt 500 ]; then
        echo "  WARNING: JS bundle seems too small (< 500 bytes)"
    else
        echo "  OK: JS bundle size looks reasonable"
    fi
else
    echo "  MISSING: JS bundle not found!"
    ERRORS=$((ERRORS + 1))
fi

# 4. MavenLocal artifacts
echo ""
echo "--- MavenLocal Artifacts ---"
ML="$HOME/.m2/repository/io/cloudsync"
if [ -d "$ML" ]; then
    echo "  MavenLocal artifacts found:"
    find "$ML" -type f -name "*.pom" -o -name "*.jar" -o -name "*.aar" -o -name "*.module" | while read f; do
        FSIZE=$(stat -c%s "$f" 2>/dev/null || stat -f%z "$f" 2>/dev/null || echo "0")
        echo "    $f ($((FSIZE / 1024))KB)"
    done
else
    echo "  No MavenLocal artifacts found (run publish-local.sh first)"
fi

# 5. Web sample bundle copy
echo ""
echo "--- Web Sample Bundle ---"
WEB_SAMPLE_JS="$PROJECT_DIR/samples/web-app/engine.js"
if [ -f "$WEB_SAMPLE_JS" ]; then
    SIZE=$(stat -c%s "$WEB_SAMPLE_JS" 2>/dev/null || stat -f%z "$WEB_SAMPLE_JS" 2>/dev/null || echo "0")
    echo "  Web sample: $WEB_SAMPLE_JS ($((SIZE / 1024))KB)"
fi

echo ""
if [ "$ERRORS" -gt 0 ]; then
    echo "=== $ERRORS validation error(s) found! ==="
    exit 1
else
    echo "=== All artifacts validated successfully! ==="
fi
