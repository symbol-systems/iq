#!/usr/bin/env bash
# bin/lib/jar-resolver.sh — shared JAR resolution and build logic
#
# Usage:
#   source `dirname $0`/lib/jar-resolver.sh
#   JAR_PATH=$(resolve_jar "iq-cli" "iq-cli-*-shaded.jar")


##############################################################################
# Resolve and/or build a JAR artifact for a given module
#
# Args:
#   $1 - module name (e.g., "iq-cli", "iq-apis")
#   $2 - jar pattern (e.g., "iq-cli-*-shaded.jar", "iq-apis-*.jar")
#
# Returns:
#   Full path to the JAR file, or exits with error if build fails
##############################################################################
resolve_jar() {
  local module=$1
  local pattern=$2
  local jar_path

  if [[ -z "$module" || -z "$pattern" ]]; then
    echo "[ERROR] resolve_jar: missing arguments (module, pattern)" >&2
    exit 1
  fi

  # Check for existing JAR
  jar_path=$(ls -1 "$module/target/$pattern" 2>/dev/null | head -n 1 || true)
  
  if [[ -n "$jar_path" ]]; then
    echo "$jar_path"
    return 0
  fi

  # JAR not found, build it
  echo "[INFO] No existing jar found. Building $module..." >&2
  if ./mvnw -pl "$module" -am package -DskipTests > /dev/null 2>&1; then
    # After build, find the JAR
    jar_path=$(ls -1 "$module/target/$pattern" 2>/dev/null | head -n 1 || true)
    if [[ -n "$jar_path" ]]; then
      echo "$jar_path"
      return 0
    else
      echo "[ERROR] resolve_jar: JAR not found after build: $pattern" >&2
      exit 1
    fi
  else
    echo "[ERROR] resolve_jar: Maven build failed for $module" >&2
    exit 1
  fi
}


##############################################################################
# Check if a command is available in PATH
#
# Args:
#   $1 - command name (e.g., "mvn", "java")
#
# Returns:
#   0 if command exists, 1 otherwise
##############################################################################
command_exists() {
  local cmd=$1
  if command -v "$cmd" > /dev/null 2>&1; then
    return 0
  else
    return 1
  fi
}


##############################################################################
# Resolve Maven command preference
#
# Prefers local `mvn` if available, otherwise uses `./mvnw` wrapper
#
# Returns:
#   Maven command (mvn or ./mvnw)
##############################################################################
resolve_maven_cmd() {
  if command_exists "mvn"; then
    echo "mvn"
  else
    echo "./mvnw"
  fi
}
