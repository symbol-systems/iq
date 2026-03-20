# IQ CLI Pro

IQ CLI Pro extends the community CLI with advanced orchestration, audit-ready connectors, and enterprise-grade workflow support.

This module is part of the `iq` mono-repo and is intended for platform operators who need a command-line reference implementation for:

- orchestrating flow-based knowledge executions
- running connector-based discovery (AWS, GitHub, SQL, etc.)
- managing hash-identified state snapshots in RDF
- enriching, rule-mapping, and pipeline debugging

## Features

- Higher-level orchestration commands compared to community edition
- Additional enterprise connectors + publisher styles
- Built-in support for CLI user goals (graph export, fail-safe agents)
- Test harness for integration audits with real credentials

## Prerequisites

- Java 21
- Maven 3.8+
- Linux/macOS/Windows (POSIX shell recommended)

## Build

```bash
./mvnw clean install -pl iq-cli-pro -am
```

Output: `iq-cli-pro/target/iq-cli-pro-*.jar`

## Usage examples

Show help:

```bash
java -jar iq-cli-pro/target/iq-cli-pro-<version>.jar help
```

Advanced IQ CLI features:

- `iq import --from ./lake --realm iq://analytics` – realm-scoped lake import
- `iq export --to ./snapshots --realm iq://analytics` – export one realm graph
- `iq sparql current-actors` – run named query from `assets/*.sparql`
- `iq script --list` and `iq script gather-data.sparql` – script listing and execution
- `iq agent --list` and `iq agent --trigger --actor planner --intent execute` – agent transition operations

Run a predefined connector flow:

```bash
java -jar iq-cli-pro/target/iq-cli-pro-<version>.jar connector run --id urn:connector:aws:example
```

Export current model:

```bash
java -jar iq-cli-pro/target/iq-cli-pro-<version>.jar model export --output out.ttl --format turtle
```

## Configuration

Settings can be provided via:

- environment variables
- `~/.iq/config` or current workspace `.iq/config`
- command arguments (highest precedence)

Common env vars:

- `IQ_CONFIG_DIR`
- `IQ_LOG_LEVEL`
- `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_REGION`
- `GITHUB_TOKEN`

## Testing

Unit test run:

```bash
./mvnw -pl iq-cli-pro test
```

Integration test run (may require real service credentials):

```bash
./mvnw -pl iq-cli-pro -DskipITs=false test
```

## Best practices

1. Keep credentials out of source control; use OS secret store or env var injection.
2. Use stable connector URIs for repeatable graph snapshotting.
3. Validate output with `rdf4j`-friendly syntax for interoperability.

## Troubleshooting

- `NoClassDefFoundError` -> verify module classpath and `-pl` filter.
- `PermissionDenied` on AWS/GitHub -> check locale credentials and permissions.

## Community

Contributions, issues, and questions are welcome at https://github.com/symbol-systems/iq
