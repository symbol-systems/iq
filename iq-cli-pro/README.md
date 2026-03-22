# iq-cli-pro — Extended Command-Line Interface

`iq-cli-pro` is the advanced edition of the IQ CLI. It extends the community edition with richer orchestration commands, additional connector integrations, and workflow tools aimed at platform operators and power users.

It is the recommended CLI for teams running IQ in production or managing multiple realms from the command line.

## What it adds over the community CLI

- Realm-scoped lake import and export
- Named SPARQL query execution and script management
- Agent listing and transition triggering
- Extended connector support for AWS, GitHub, JDBC, and others
- Audit-ready command history and structured output

## Starting the CLI

```bash
./bin/iq-cli-pro
```

## Installing as a standalone tool

```bash
./bin/install-cli-pro
```

This builds the CLI, copies the jar to `~/bin/`, and installs a launcher script so you can run `iq` from anywhere on your path — no Maven required after installation.

## Requirements

- Java 21
- Maven (wrapper included, only needed during build)
- Linux, macOS, or Windows with a POSIX-compatible shell

## Configuration

CLI Pro reads from environment variables and the `.iq/` directory. Supported variables:

- `IQ_CONFIG_DIR` — override the config location
- `IQ_LOG_LEVEL` — adjust verbosity
- `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_REGION`
- `GITHUB_TOKEN`

Credentials should never be stored in source control. Use environment variable injection or an OS secret store.

## Part of the IQ family

`iq-cli-pro` is part of the same mono-repo as the rest of IQ. It can be built and run independently, or as part of a full platform build.
