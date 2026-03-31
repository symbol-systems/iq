# pom.xml Reference

The iq-starter is not a Maven module itself, but rather a **packaging** of the existing IQ runtime.

When you run scripts in `bin/`, they:

1. Navigate to the repository root (`../../`)
2. Run Maven commands using the root `pom.xml`
3. Build specific modules (iq-apis, iq-cli, iq-mcp)
4. Execute the built artifacts

## Building IQ from source

```bash
# Full build (all modules)
cd ../..
./mvnw clean install

# Build specific modules
./mvnw -pl iq-apis -am clean install
./mvnw -pl iq-cli -am clean install
./mvnw -pl iq-mcp -am clean install
```

## Using pre-built artifacts

If you have the Maven artifacts already built, the scripts in `bin/` will use them directly without rebuilding.

## No new modules

This starter kit introduces **no new code or modules**. It's pure:
- Documentation
- Examples
- Configuration
- Scripts that wrap existing runtimes

This keeps the codebase clean and focused on the core IQ platform.

---

For the actual Maven configuration, see the root [pom.xml](../../pom.xml).
