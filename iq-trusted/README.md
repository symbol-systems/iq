# iq-trusted — Auth, JWT, Secrets, and Trust

`iq-trusted` is IQ's security and identity layer. It handles JWT token generation and validation, secrets management, key storage, and the trust relationships that govern what each realm and agent is allowed to do.

## What it provides

- **JWT minting** — creates realm-scoped bearer tokens that authorise API calls, with configurable expiry and audience claims
- **VFSPasswordVault / EnvsAsSecrets** — pluggable secrets backends: load credentials from encrypted vault files or from environment variables
- **VFSKeyStore** — manages cryptographic key pairs per realm, stored on the Virtual File System
- **Trust connectors** — integrations with external identity providers: GitHub, Discord, Web3, LinkedIn, and mobile auth flows
- **TrustedPlatform** — realm bootstrapping with trust-aware agent initialisation
- **MY_IQ_AI** — the trusted AI runtime entry point, runnable standalone

## Running the standalone trusted AI entry point

```bash
./bin/iq-ai
```

This compiles and launches the trusted AI runner — useful for testing agent behaviour with a fully initialised trust context.

## Security model

Each realm gets its own JWT token written to `.iq/jwt/` at startup. API calls must present this token as a bearer credential. Token duration is configurable via `iq.realm.jwt.duration` (default: 30 days).

Secrets are never committed to source control. Use `EnvsAsSecrets` for container and CI environments, or `VFSPasswordVault` for local encrypted storage.

## Requirements

- Java 21
- Part of the IQ mono-repo; build with `./mvnw -pl iq-trusted -am compile`
