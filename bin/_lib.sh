#!/usr/bin/env bash
# Shared utility functions for bin/ scripts
# Source this file: source "$(dirname "$0")/_lib.sh"

set -euo pipefail

# Find and run a JAR with build fallback
# Usage: find_and_run_jar <module> <main_class> <target_pattern> [java_opts] [args...]
# Example: find_and_run_jar "iq-cli" "systems.symbol.CLI" "iq-cli-*-shaded.jar" "" "$@"
find_and_run_jar() {
  local module="$1"
  local main_class="$2"
  local jar_pattern="$3"
  local java_opts="${4:-}"
  shift 4
  local args=("$@")

  local root
  root="$(cd "$(dirname "$0")/.." && pwd)"
  local target_dir="$root/$module/target"

  # Try to find existing JAR
  local jar_path=""
  if [[ -d "$target_dir" ]]; then
    jar_path=$(find "$target_dir" -maxdepth 1 -name "$jar_pattern" -type f | head -n 1 || true)
  fi

  # Build if not found
  if [[ -z "$jar_path" ]]; then
    echo "[INFO] No built jar found for $module. Building..." >&2
    cd "$root"
    ./mvnw -pl "$module" -am package -DskipTests >/dev/null 2>&1 || {
      echo "[ERROR] Failed to build $module" >&2
      return 1
    }
    jar_path=$(find "$target_dir" -maxdepth 1 -name "$jar_pattern" -type f | head -n 1 || true)
  fi

  if [[ -z "$jar_path" ]]; then
    echo "[ERROR] Could not find jar matching pattern: $jar_pattern" >&2
    echo "[INFO] Falling back to maven exec:java..." >&2
    cd "$root"
    ./mvnw compile exec:java -pl "$module" -am \
      -Dexec.mainClass="$main_class" \
      -DskipTests \
      "${args[@]}" >/dev/null 2>&1 || {
        echo "[ERROR] Maven exec also failed" >&2
        return 1
      }
    return 0
  fi

  echo "[INFO] Running: $jar_path" >&2
  cd "$root"
  java ${java_opts:+$java_opts} -jar "$jar_path" "${args[@]}"
}

# Run maven with error checking
# Usage: run_maven <maven_args...>
run_maven() {
  set +e
  ./mvnw "$@"
  local exit_code=$?
  set -e
  if [[ $exit_code -ne 0 ]]; then
    echo "[ERROR] Maven command failed with exit code $exit_code" >&2
    return $exit_code
  fi
}

export -f find_and_run_jar run_maven
