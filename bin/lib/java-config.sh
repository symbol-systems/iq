#!/usr/bin/env bash
# bin/lib/java-config.sh — centralized Java environment configuration
#
# Usage:
#   source `dirname $0`/lib/java-config.sh
#   init_java_logging
#   init_java_opts


##############################################################################
# Initialize logging configuration for all CLI tools
#
# Sets up JDK logging manager (standard, not JBoss) for consistent
# behavior across iq-cli, iq-cli-pro, iq-cli-server, iq-mcp
##############################################################################
init_java_logging() {
  export JAVA_OPTS="${JAVA_OPTS:-} -Djava.util.logging.manager=java.util.logging.LogManager"
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.util.logging.manager=java.util.logging.LogManager"
  # Ensure they're in the environment for child processes
  return 0
}


##############################################################################
# Initialize common Java options
#
# Sets up:
# - Memory settings (optional, can be overridden by IQ_JAVA_OPTS env var)
# - Encoding (UTF-8)
# - Other standard flags
##############################################################################
init_java_opts() {
  # Allow override via environment variable
  if [[ -n "${IQ_JAVA_OPTS:-}" ]]; then
    export JAVA_OPTS="${JAVA_OPTS:-} ${IQ_JAVA_OPTS}"
  fi
  
  # Set UTF-8 encoding
  export JAVA_OPTS="${JAVA_OPTS:-} -Dfile.encoding=UTF-8"
  
  # Allow remote debugging if IQ_DEBUG=true
  if [[ "${IQ_DEBUG:-false}" == "true" ]]; then
    export JAVA_OPTS="${JAVA_OPTS:-} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
    echo "[DEBUG] Java debugger listening on port 5005" >&2
  fi
  
  return 0
}


##############################################################################
# Initialize all Java configuration
#
# Combines logging and Java options setup in one call
##############################################################################
init_java_config() {
  init_java_logging
  init_java_opts
  return 0
}
