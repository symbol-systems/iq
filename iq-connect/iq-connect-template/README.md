# IQ Connector Template

This module serves as a minimal template to build a new IQ connector. It demonstrates:

- `I_Connector` implementation exposing an RDF `Model` state.
- `I_ConnectorKernel` implementation driving connector lifecycle.
- `I_Checkpoint` usage (via helper utilities in `iq-connect-core`).

## How to use

1. Copy/extend `TemplateConnector` and implement real sync logic in `refresh()`.
2. Use `Checkpoints.of(model)` to create checkpoints and apply them when restarting.
3. Register your connector via the `I_ConnectorRegistry` implementation used by your runtime.
