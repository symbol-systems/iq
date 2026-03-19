# IQ Connector: Template

## Purpose

This module provides a minimal reference implementation demonstrating the core IQ connector pattern:
- Connector state stored as RDF `Model`.
- Sync lifecycle driven by a connector kernel.
- Checkpointing and restore via `I_Checkpoint`.

## Architecture

- **TemplateConnector**: Implements `I_Connector`. Exposes a state `Model` and implements basic lifecycle.
- **TemplateKernel**: Implements `I_ConnectorKernel` and delegates lifecycle calls to the `I_Connector`.

## How to use

1. Copy `TemplateConnector` into your own connector module.
2. Implement `refresh()` to perform your sync logic.
3. Use `Checkpoints.of(model)` from `iq-connect-core` to snapshot state.
4. Register your connector in IQ via `I_ConnectorRegistry`.

## Notes

- This module is intentionally minimal and has no external API dependencies beyond RDF4J and `iq-connect-core`.
- Use this template as a starting point for any new connector implementation.
