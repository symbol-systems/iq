# Maven Build Profiles

This project uses Maven profiles to segment the build into different product editions and development scenarios. Profiles control which modules are compiled and packaged.

## Active Profiles

### OSS Profile (Default)
**Activation:** Active by default when no `-P` flag is specified  
**Flag:** `-Poss`  
**Purpose:** Fast development builds of core open-source framework  
**Modules (11 total):**
- iq-abstract
- iq-kernel
- iq-aspects
- iq-secrets
- iq-rdf4j
- iq-intents
- iq-platform
- iq-lake
- iq-rdf4j-graphs
- iq-rdf4j-graphql
- 4 OSS connectors (core, template, github, aws)

**Build Time:** ~60-90 seconds (on typical hardware)  
**Use Case:** Local development, quick feedback loops

```bash
./bin/build-oss -DskipTests clean compile
# or simply:
./mvnw clean install
```

### Pro Profile
**Flag:** `-Ppro`  
**Purpose:** Professional edition with all OSS modules + commercial modules  
**Modules (24 total):**
- All OSS modules (11 above)
- iq-cli (requires iq-apis)
- iq-test-servers
- iq-cli-pro
- iq-cli-server
- iq-agentic
- iq-onnx
- iq-trusted
- iq-camel
- iq-apis
- iq-mcp

**Build Time:** ~100-150 seconds  
**Use Case:** Full platform builds, CI/CD pipelines, production deployment preparation

```bash
./bin/build-pro clean install
# or:
./mvnw -Ppro clean install
```

### Enterprise Profile
**Flag:** `-Penterprise`  
**Purpose:** Full platform with reserved extension points for HA/governance features  
**Modules:** Same as Pro (24 total) + placeholder for enterprise-specific modules  
**Build Time:** ~100-150 seconds  
**Use Case:** Enterprise deployments, future multi-tenancy, governance features

```bash
./bin/build-enterprise clean install
# or:
./mvnw -Penterprise clean install
```

### Addons Profile
**Flag:** `-Paddons`  
**Purpose:** Experimental and optional modules built with full core framework  
**Modules (17 total):**
- All OSS core modules (11)
- 4 OSS connectors
- iq-lab
- iq-skeleton
- iq-tokenomic

**Build Time:** ~120-180 seconds  
**Use Case:** Development of new features, experimentation, prototyping

```bash
./bin/build-addons clean install
./mvnw -Paddons clean install
```

## Quick Build Reference

```bash
# Fast OSS development build (skips tests)
./bin/build-oss -DskipTests clean compile

# Full PRO build with tests
./bin/build-pro clean verify

# Enterprise build
./bin/build-enterprise -DskipTests install

# Addons with skip integration tests
./bin/build-addons -DskipITs clean test

# Individual module within a profile
./mvnw -Ppro -pl iq-apis -am compile

# Build specific module + its dependencies
./mvnw -pl iq-platform -am clean test
```

## Dependency Graph

**Build Order in Profiles:**
1. iq-abstract (base constants, no internal deps)
2. iq-kernel (depends on iq-abstract)
3. iq-aspects (depends on iq-kernel)
4. iq-secrets (depends on iq-aspects)
5. iq-rdf4j (depends on iq-abstract, iq-kernel, iq-aspects)
6. iq-intents (depends on iq-rdf4j)
7. iq-platform (depends on multiple core modules)
8. iq-lake (depends on iq-platform and iq-rdf4j)
9. iq-rdf4j-graphs (depends on iq-rdf4j)
10. iq-rdf4j-graphql (depends on iq-rdf4j)
11-14. OSS Connectors (core, template, github, aws)
15+. PRO modules (cli, test-servers, apis, mcp, etc.)

**Pro-only Dependencies:**
- iq-cli → iq-apis
- iq-test-servers → iq-apis
- iq-apis → iq-platform

## Convenience Scripts

Four build scripts are provided in `./bin/`:

- **build-oss** - `./mvnw -Poss "$@"`
- **build-pro** - `./mvnw -Ppro "$@"`
- **build-enterprise** - `./mvnw -Penterprise "$@"`
- **build-addons** - `./mvnw -Paddons "$@"`

These wrap Maven with the appropriate profile flag and accept all standard Maven arguments.

## Troubleshooting

### Profile Not Activated
Check that you're using the correct flag syntax:
```bash
./mvnw -Ppro clean compile  # Correct
./mvnw -P pro clean compile # Wrong - must not have space after -P
```

### Module Not Compiling
Verify the module is in your chosen profile. For example, iq-cli is only in Pro, not OSS.

### Dependency Resolution Fails
Clear Maven cache and retry:
```bash
rm -rf ~/.m2/repository/systems/symbol  # Clear symbol.systems artifacts
./bin/build-pro clean install
```

### Reference
See [MODULES.md](./MODULES.md) for detailed architecture and rationale behind profile segmentation.
