# iq-mcp: IQ Model Context Protocol Integration

MCP (Model Context Protocol) is a standardized protocol for connecting LLMs to IQ's data sources, Agents, APIs, and tools.

## Security note

The built-in `AuthGuardMiddleware` currently performs a **best-effort principal extraction** from a bearer JWT (it parses the `sub` claim from the JWT payload) but **does not validate signatures or issuers**. This implementation is intended for local development and testing only. For production use, hook in a real JWT validator (e.g., Nimbus JOSE JWT, Spring Security) via the `AuthGuardMiddleware(JwtPrincipalExtractor)` constructor.

