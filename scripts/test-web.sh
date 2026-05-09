#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "=== Building and testing Web sample ==="
cd "$PROJECT_DIR"

echo ""
echo "--- Build JS webpack bundle ---"
./gradlew :engine:jsWebBundle --no-daemon -q

JS_BUNDLE="engine/build/outputs/js/engine.js"
if [ ! -f "$JS_BUNDLE" ]; then
    echo "ERROR: JS bundle not found at $JS_BUNDLE"
    exit 1
fi
echo "JS bundle size: $(ls -lh "$JS_BUNDLE" | awk '{print $5}')"

echo ""
echo "--- Copy bundle to web sample ---"
cp "$JS_BUNDLE" samples/web-app/engine.js
echo "Bundle copied to samples/web-app/engine.js"

echo ""
echo "--- Validate bundle with Node.js ---"
cd samples/web-app
node -e "
try {
    const fs = require('fs');
    const content = fs.readFileSync('engine.js', 'utf8');
    console.log('Bundle loaded: ' + content.length + ' bytes');
    console.log('File exists and is valid UTF-8');
    console.log('First 200 chars:');
    console.log(content.substring(0, 200));
} catch(e) {
    console.error('Validation error:', e.message);
    process.exit(1);
}
"

echo ""
echo "--- Validate HTML structure ---"
if [ -f "index.html" ]; then
    if grep -q "CloudSyncEngine" index.html; then
        echo "index.html references CloudSyncEngine correctly"
    fi
    if grep -q "engine.js" index.html; then
        echo "index.html loads engine.js correctly"
    fi
fi

cd "$PROJECT_DIR"
echo ""
echo "=== Web sample validation completed! ==="
