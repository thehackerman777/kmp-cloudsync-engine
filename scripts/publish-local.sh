#!/usr/bin/env bash
set -euo pipefail

# Publishes all artifacts to MavenLocal
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "=== Publishing all modules to MavenLocal ==="
cd "$PROJECT_DIR"

echo ""
echo "--- Building and publishing engine module ---"
./gradlew :engine:publishToMavenLocal --no-daemon -q

echo ""
echo "--- Verify MavenLocal artifacts ---"
MAVEN_LOCAL="$HOME/.m2/repository/io/cloudsync"
if [ -d "$MAVEN_LOCAL" ]; then
    echo "Artifacts found in MavenLocal:"
    find "$MAVEN_LOCAL" -name "*.jar" -o -name "*.aar" -o -name "*.pom" | sort
else
    echo "WARNING: No artifacts found in MavenLocal."
fi

echo ""
echo "=== Done! ==="
