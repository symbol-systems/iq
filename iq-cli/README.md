# IQ CLI (Community)

IQ CLI is the command-line interface for the IQ platform. It's a lightweight Java-based client to manage IQ knowledge graph, connectors, and automated playbooks.

## Key Features

- Discover and list connector types (`aws`, `github`, `sparql`, etc.)
- Execute scan and refresh operations from the terminal
- Inspect and serialize RDF models in Turtle/JSON-LD formats
- Work with local and remote repositories (vault + RDF4J endpoints)

## Prerequisites

- Java 21
- Maven 3.8+
- Git clone of the repository

## Build

From repository root:

```bash
./mvnw clean install -pl iq-cli -am
```

This produces `iq-cli/target/iq-cli-*.jar`.

## Run

Basic command syntax:

```bash
java -jar iq-cli/target/iq-cli-<version>.jar <command> [options]
```

Example: show help

```bash
java -jar iq-cli/target/iq-cli-<version>.jar help
```

## Common Workflows

- Start a local state repository:
  - Configure `.iq/config` / `iq-cli` settings
- Run a connector:
  - `java -jar ... connector run --id urn:connect:aws --provider aws --region us-east-1`
- Export model to Turtle:
  - `java -jar ... model export --format turtle --out out.ttl`

## New/Improved commands (2026 update)

- `iq import --from /path/to/assets --realm iq://myrealm` (load lake by realm context)
- `iq export --to /path/to/export --realm iq://myrealm` (dump only given realm graph)
- `iq sparql <query-name|file|"SELECT ...">` (run local SPARQL or inline query)
- `iq script --list` (list scripts under `assets/scripts`)
- `iq script <script>.sparql` (run script directly)
- `iq agent --list [--actor foo] [--intent bar]` (list discovered agent transition steps)
- `iq agent --trigger --actor foo --intent bar` (stubbed trigger path; integration points to IntentAPI TODO)

## Scripts and agent transition workflows

- Store agent transition definitions in `assets/scripts`.
- Run via `iq script` to inspect and execute.
- Manage runtime transitions with `iq agent` for orchestration tracing.

## Config

CLI settings are read from ENV and `.iq` config files. Preferred environment variables:

- `IQ_HOME`
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `GITHUB_TOKEN`

## Testing

Run unit tests:

```bash
./mvnw -pl iq-cli test
```

## Troubleshooting

- If your JAR does not start: ensure Java 21 is active.
- If connectors are not discovered: run `java -jar ... connector list` and check configured connectors in `iq-connect` modules.

## GitHub

Project: `https://github.com/symbol-systems/iq`

Contributions welcome via issues/PRs.
