# IQ Connector Status

This document tracks the implementation readiness and feature completeness of each IQ Connect module.

## Legend

| Status | Meaning |
|--------|---------|
| 🟢 **Ready** | Production-ready with comprehensive API coverage |
| 🟡 **Partial** | Implemented but incomplete; limited feature set |
| 🔵 **In Progress** | Active development with core infrastructure in place |
| ⚪ **Scaffolding** | Template/reference only; framework but no live implementation |
| ⚙️ **Planned** | No source code yet; awaiting implementation |

## Connector Matrix

| Connector | Status | Java Files | Key Features | Notes |
|-----------|--------|------------|--------------|-------|
| **iq-connect-aws** | 🟡 Partial | 11 | S3, Lambda, IAM basic ops | Focuses on S3 object read/write; Lambda invocation stubbed; uses AWS SDK v2 |
| **iq-connect-azure** | ⚙️ Planned | 0 | — | Prioritized for enterprise; awaiting impl. See `iq-connect-azure/README.md` for design intent |
| **iq-connect-confluenc** | ⚙️ Planned | 0 | — | Wiki/document retrieval; no API integration yet |
| **iq-connect-core** | 🟢 Ready | 19 | Base interfaces, pipeline, registry | All connector modules inherit from this; stable API |
| **iq-connect-databricks** | ⚙️ Planned | 0 | — | Delta Lake tables + SQL; awaiting Databricks SDK integration |
| **iq-connect-datadog** | ⚙️ Planned | 0 | — | Metrics + logs API retrieval; monitoring dashboard queries; planned but not started |
| **iq-connect-digitalocean** | ⚙️ Planned | 0 | — | Droplets, volumes, databases; lower priority |
| **iq-connect-docker** | ⚙️ Planned | 0 | — | Container image ops, registry access; awaiting Docker client integration |
| **iq-connect-gcp** | ⚙️ Planned | 0 | — | BigQuery, Firestore, Storage; lower priority; see Azure first |
| **iq-connect-github** | 🟡 Partial | 9 | Repos, issues, PRs, gists | Basic GitHub v3 REST API bindings; OAuth2 not fully secured; rate limiting basic |
| **iq-connect-google-apps** | ⚙️ Planned | 0 | — | Sheets, Drive, Docs; OAuth scopes; on roadmap |
| **iq-connect-k8s** | ⚙️ Planned | 0 | — | Workload queries, pod exec, logs; await Kubernetes Java client setup |
| **iq-connect-office-365** | ⚙️ Planned | 0 | — | Teams channels, SharePoint lists, OneDrive; O365 SDK integration required |
| **iq-connect-parquet** | ⚙️ Planned | 0 | — | Parquet file read/schema inspection; local file system or S3 URIs |
| **iq-connect-salesforce** | ⚙️ Planned | 0 | — | SOQL queries, object CRUD; awaiting Salesforce REST client |
| **iq-connect-slack** | ⚙️ Planned | 0 | — | Channel messages, user info, file upload; Bolt SDK on roadmap (has .md design) |
| **iq-connect-snowflake** | ⚙️ Planned | 0 | — | SQL warehouse queries, metadata; high priority; awaiting SDK integration |
| **iq-connect-stripe** | ⚙️ Planned | 0 | — | Payment/customer queries; lower priority |
| **iq-connect-template** | ⚪ Scaffolding | 2 | Base connector class template | Code template + stub for new connector implementations |

---

## Development Guidelines

### Adding a New Connector

1. **Create module** from template:
   ```bash
   cp -r iq-connect-template iq-connect-<service>
   sed -i 's/Template/<Service>/g' iq-connect-<service>/pom.xml iq-connect-<service>/src/main/java/**/*.java
   ```

2. **Define operations** in a service interface extending `IConnector`:
   ```java
   public interface I<Service> extends IConnector {
       List<Item> query(String filter);
       boolean create(Item item);
       // ...
   }
   ```

3. **Implement the interface** with SDK calls and error handling.

4. **Register in the MCP registry** — add capability routes in `iq-mcp`.

5. **Write tests** — at minimum, mock API tests; integration tests optional behind `@Tag("integration")`.

6. **Update this STATUS.md** — change status and file count.

### Recommended Implementation Order

1. 🔴 **Critical** (blocks other work):
   - ✅ iq-connect-core (scaffolding)
   - ⏳ iq-connect-azure (enterprise requirement)
   - ⏳ iq-connect-snowflake (data warehouse; high impact)

2. 🟠 **High Priority** (most useful):
   - ⏳ iq-connect-slack (team communication)
   - ⏳ iq-connect-github (code collaboration)
   - ⏳ iq-connect-databricks (analytics)

3. 🟡 **Low Priority** (nice-to-have):
   - iq-connect-gcp, iq-connect-stripe, iq-connect-digitalocean, etc.

---

## Known Issues

- **iq-connect-aws**: Lambda client not fully wired; S3 edge cases (large files, multipart) untested.
- **iq-connect-github**: OAuth scope validation is permissive; rate limits not enforced on client side.
- **iq-connect-template**: Instructions incomplete; refer to iq-connect-core README for API.

---

## Further Reading

- [iq-connect-core README](iq-connect-core/README.md) — Connector API design, interfaces, and lifecycle
- [iq-connect-aws README](iq-connect-aws/README.md) — AWS-specific configuration and examples
- [iq-connect-azure README](iq-connect-azure/README.md) — Design intent for Azure connectors
- [iq-connect-slack README](iq-connect-slack/README.md) — (Planned) Slack integration design document

