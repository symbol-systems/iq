# iq-cli-pro — Extended Command-Line Interface

`iq-cli-pro` is the advanced edition of the IQ CLI. It extends the community edition with richer orchestration commands, additional connector integrations, and workflow tools aimed at platform operators and power users.

It is the recommended CLI for teams running IQ in production or managing multiple realms from the command line.

## What it adds over the community CLI

- Realm-scoped lake import and export
- Named SPARQL query execution and script management
- Agent listing and transition triggering
- Extended connector support for AWS, GitHub, JDBC, and others
- Audit-ready command history and structured output
- **Trust Management**: PKI-based trust relationships with signature verification and OAuth integration
  - `iq trust me` — Self-signing with RSA-2048 keypair
  - `iq trust <DID>` — Establish trust with remote identities
  - `iq trust <DID> --sig <signature>` — Verify cryptographic signatures
  - `iq trust --provider github` — OAuth-based trust via external providers
  - `iq trust list [--detail]` — Enumerate and audit trust relationships
  - `iq trust <DID> --revoke` — Revoke established trust

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
- `GITHUB_TOKEN` — OAuth token for GitHub trust relationships
- `GOOGLE_TOKEN` — OAuth token for Google trust relationships  
- `MICROSOFT_TOKEN` — OAuth token for Microsoft trust relationships
- `GITHUB_USER` — GitHub username for trust resolution
- `GOOGLE_USER` — Google user email for trust resolution
- `MICROSOFT_USER` — Microsoft UPN for trust resolution

Credentials should never be stored in source control. Use environment variable injection or an OS secret store.

### Trust Management Configuration

The trust command automatically manages PKI keypairs and stores signatures in RDF. For production deployments:

1. Set OAuth tokens via environment variables or `.iq/vault` 
2. Configure key storage strategy (filesystem, HSM, or cloud vault)
3. Enable audit logging: `IQ_LOG_LEVEL=DEBUG`

See [TRUST_IMPLEMENTATION.md](../TRUST_IMPLEMENTATION.md) for detailed PKI setup and usage.

## Part of the IQ family

`iq-cli-pro` is part of the same mono-repo as the rest of IQ. It can be built and run independently, or as part of a full platform build.
