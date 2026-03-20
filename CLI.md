# IQ CLI Installation and Runtime

This document explains how to install and use the IQ CLI and runtime in your environment.

## 1. Requirements

- Linux, macOS, or Windows
- Java 21 (JDK 21 or newer)
- Maven (recommended)
- Git

## 2. Get the repository

1. `git clone https://github.com/symbol-systems/iq.git`
2. `cd iq`

## 3. Build and install the CLI

The repository includes a command-line frontend for managing IQ.

1. On Linux/macOS:
   - `./mvnw clean install -DskipTests` (builds all modules including CLI)
   - The CLI executable is under `iq-cli/target` or `iq-cli-pro/target` depending on edition.
   - Optionally make wrapper script executable: `chmod +x bin/iq`

2. On Windows:
   - `mvnw.cmd clean install -DskipTests`
   - Use `in\iq` or the generated JAR in `iq-cli\target`.

## 4. Run the CLI

- Basic command: `./bin/iq --help`
- Example: `./bin/iq connect list` (depending on installed connectors)

## 5. Run runtime APIs

For API mode, use module `iq-apis`:

- `./mvnw -pl iq-apis -am compile quarkus:dev` starts local dev API server.
- Access API docs at `http://localhost:8080/q/dev/`.

## 6. Quick short-cuts

- `./bin/compile-apis` builds just API modules fast.
- `./bin/build-image` builds Docker image when ready to deploy.

## 7. Server lifecycle commands (even with iq-cli-server)

- `java -jar iq-cli-server/target/iq-cli-server.jar server api start`
- `java -jar iq-cli-server/target/iq-cli-server.jar server api health --verbose`
- `java -jar iq-cli-server/target/iq-cli-server.jar server mcp stop`

## 8. Troubleshooting

- Ensure JAVA_HOME points to JDK 21+.
- Ensure Maven can download dependencies from the internet.
- When no command found, ensure `bin/iq` is in your PATH.
