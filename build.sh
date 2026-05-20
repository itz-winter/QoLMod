#!/usr/bin/env bash
# build.sh — Cross-platform build helper for QoLMod
# Usage:
#   ./build.sh              — build all versions
#   ./build.sh 1.21.11      — build a specific version
#   ./build.sh clean        — clean all build outputs
set -euo pipefail

VERSIONS=(
  1.16.5 1.17.1 1.18.2
  1.19.2 1.19.4
  1.20.1 1.20.4 1.20.6
  1.21.1 1.21.3 1.21.4 1.21.5 1.21.6 1.21.7 1.21.8 1.21.9 1.21.10 1.21.11
)

GRADLEW="./gradlew"
[[ -x "$GRADLEW" ]] || { echo "gradlew not found or not executable. Run from project root."; exit 1; }

case "${1:-}" in
  "")
    echo "Building all versions..."
    TASKS=()
    for v in "${VERSIONS[@]}"; do
      TASKS+=(":versions:${v}:build")
    done
    "$GRADLEW" "${TASKS[@]}" -x test
    ;;

  clean)
    echo "Cleaning all build outputs..."
    "$GRADLEW" clean
    ;;

  *)
    VERSION="$1"
    echo "Building version ${VERSION}..."
    "$GRADLEW" ":versions:${VERSION}:build" -x test
    ;;
esac

echo ""
echo "Build complete. Locating JARs..."
for v in "${VERSIONS[@]}"; do
  dir="versions/${v}/build/libs"
  [[ -d "$dir" ]] || continue
  while IFS= read -r -d '' jar; do
    echo "  $jar"
  done < <(find "$dir" -name "*.jar" -not -name "*-dev.jar" -not -name "*-sources.jar" -print0)
done
