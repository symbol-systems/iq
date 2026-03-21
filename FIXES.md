# FIXES.md

This file consolidates all `TODO`, `@Disabled`, `not implemented`, and missing-feature markers found in the repository at 2026-03-20.

It is intended as a single source of truth for unresolved behavior and planned actions.

## Summary

- Total TODO markers found: 19
- Disabled tests: 0 (@Disabled markers not found)
- Explicit `not implemented` markers: 0 (as text), plus 6 `UnsupportedOperationException` handlers in runtime manager
- Explicit unsupported behavior references: 12

## 🔴 Open items that require implementation / hardening

1. `iq-apis/src/main/java/systems/symbol/controller/trust/TokenAPI.java`
   - TODOs:
     - authenticate (subject is a user, subject known to issuer)
     - authorize (subject known to audience)
   - Status: security-critical workflow flagged, should be implemented as full OAuth/JWT subject check.
   - Action: add test coverage for missing/invalid user and audience.

2. `iq-trusted/src/main/java/systems/symbol/platform/TrustedPlatform.java`
   - TODO: `I_Self.trust(name) && name.length()>` (trust validation never executed)
   - Status: validation incomplete; implement endpoint/authz checks, plus unit tests.

3. `iq-camel/src/main/java/coded/claims/camel/routing/IQRoutePolicy.java`
   - TODO: Check ACL
   - Status: no access-control check currently on route startup; map ACL to Realm ACL plugin.

4. `iq-cli-pro/src/main/java/systems/symbol/cli/TriggerCommand.java`
   - TODO: Wire-up Apache Camel
   - Status: agent/trigger implementation stub; integrate with IntentAPI and additional tests.

5. `iq-aspects/src/main/java/systems/symbol/bean/XSD2POJOConverter.java`
   - TODO: replace with GSON
   - Status: non-blocking migration goal; can remain for graceful deprecation.

6. `iq-platform/src/main/java/systems/symbol/research/SitemapScan.java`
   - TODO: fix the hangs
   - Status: potential indefinite web crawler hang; add timeouts + retry breakouts.

7. `iq-rdf4-graphql/src/main/java/systems/symbol/gql/SPARQLDataFetcher.java`
   - TODO: Object context = environment.getContext();
   - Status: GraphQL request context not fed; implement authorization and tenant context.

8. `iq-rdf4j/src/main/java/systems/symbol/rdf4j/store/SelfModel.java`
   - TODO: what is this?
   - Status: Code smell and uninvestigated logic; document intent and clean.

9. `iq-platform/src/main/java/systems/symbol/platform/runtime/ServerRuntimeManager.java`
   - methods throw `UnsupportedOperationException("... not implemented")` for start/stop/reboot/health/debug/dump.
   - Status: clearly flagged unimplemented runtime manager features.

10. `iq-cli/src/main/java/systems/symbol/cli/ScriptCommand.java`
    - message: "Groovy scripts are not supported in iq-cli. Use iq-cli-pro for Groovy execution."
    - Status: feature domain is intentionally limited, but should have explicit tests and documentation.

11. `iq-lab/src/main/java/systems/symbol/persona/Persona.java`
    - message: "Microphone not supported!"
    - Status: platform capability limitation; document as non-implemented optional feature.

12. `iq-rdf4-graphs/src/main/java/systems/symbol/jgraph/ModelGraph.java`
    - UnsupportedOperationException: "Edge weights are not supported in RDF models."
    - Status: business-logic limitation; may be acceptable or to expand later.

## 🧩 Documentation and legacy TODOs

- `iq-cli-pro/TODO.md` still contains the TriggerCommand stub entry.
- `iq-cli/README.md` mentions stubbed `iq agent --trigger` path.
- `UPGRADE.md` has explicit red and orange issue rows associated with 1-9 above.

## 🧹 Danger: no gaps approach

Step 1: convert each entry above into a ticket/PR with:
- required behavior
- minimal reproduction unit test
- planned implementation details
- acceptance criteria

Step 2: implement in code and close TODO markers (remove comment + add tests).

Step 3: validate with full `./mvnw clean test -DskipITs=true` + targeted integration.

Step 4: mark in this file with `✅ done` when fixed.

## 📝 Proposed file status tracking format

- [ ] TokenAPI authz (security)  
- [ ] TrustedPlatform trust logic  
- [ ] IQRoutePolicy ACL enforcement  
- [ ] TriggerCommand Camel wiring  
- [ ] SitemapScan hang fix  
- [ ] SPARQLDataFetcher context wiring  
- [ ] ServerRuntimeManager runtime actions  
- [ ] SelfModel clarification  
- [ ] XSD2POJOConverter replace/GSON migration

## 🧪 Validation checks performed

- `git grep -n "TODO"` all modules (19 entries)
- `git grep -n "@Disabled"` zero entries
- `git grep -n "not implemented"` zero entries in general text
- `git grep -n "UnsupportedOperationException"` 12 entries

---
