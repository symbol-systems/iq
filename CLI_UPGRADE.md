# CLI Upgrade Plan

This file summarizes CLI upgrade requirements for `iq-cli`, `iq-cli-pro`, and `iq-cli-server`.

## 1. New module
- `iq-cli-server` added for server runtime management.
- Uses `iq-platform` `ServerRuntimeManager` and `ServerRuntimeManagerFactory`.

## 2. Runtime commands
- `iq server api/start|stop|reboot|status|health|debug|dump`
- `iq server mcp/...` (mirrors api commands)

## 3. Integration hooks
- `iq-apis` adds `/platform/runtime` endpoints.
- `iq-mcp` adds `server.runtime` tool.
- `iq-cli` adds `server` command dispatch to `iq-cli-server`.

## 4. Testing
- Unit tests in `iq-cli`, `iq-cli-server`, `iq-mcp`.
- Manual commands: `./bin/iq server --help`, `./bin/iq server api health`.

## 5. Notes
- Root build currently requires full module set present (`iq-rdf4j-graphs`, valid older `iq-parent`).
