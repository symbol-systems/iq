# IQ Logging Configuration Fixes

## Summary of Issues Addressed

This document outlines the fixes applied to resolve the logging and runtime warnings observed in the IQ APIs runtime.

### Issues Identified and Fixed

#### 1. **JBoss LogManager Initialization Error**
**Problem:** 
```
ERROR: The LogManager accessed before the "java.util.logging.manager" system property was set 
to "org.jboss.logmanager.LogManager". Results may be unexpected.
```

**Root Cause:** The JBoss logging system wasn't being initialized early enough in the startup sequence.

**Solution Applied:**
- Enabled `quarkus.log.manager=org.jboss.logmanager.LogManager` in `application.properties` 
- Created `logback.xml` configuration to properly initialize the logging framework at startup
- Explicitly suppressed the JBoss logger warnings in the logback configuration

**Files Changed:**
- `iq-apis/src/main/resources/application.properties` - Uncommented log manager setting
- `iq-apis/src/main/resources/logback.xml` - New file with proper logging configuration

---

#### 2. **SLF4J Multiple Service Provider Bindings**
**Problem:**
```
SLF4J(W): Class path contains multiple SLF4J providers.
SLF4J(W): Found provider [ch.qos.logback.classic.spi.LogbackServiceProvider]
SLF4J(W): Found provider [org.slf4j.impl.JBossSlf4jServiceProvider]
```

**Root Cause:** Both Logback and JBoss SLF4J implementations were on the classpath due to transitive dependencies from Quarkus.

**Solution Applied:**
- Added explicit exclusions to `quarkus-core` dependency in `iq-apis/pom.xml`
- Excluded:
  - `org.slf4j:slf4j-jboss-logmanager`
  - `org.jboss.slf4j:jboss-slf4j`
  
This ensures only Logback is used as the SLF4J implementation.

**Files Changed:**
- `iq-apis/pom.xml` - Added exclusions to quarkus-core dependency

---

#### 3. **ONNX Runtime Native Access Warnings**
**Problem:**
```
WARNING: A restricted method in java.lang.System has been called
WARNING: java.lang.System::load has been called by ai.onnxruntime.OnnxRuntime
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
```

**Root Cause:** ONNX Runtime uses `System.load()` to load native libraries, which requires explicit permission in Java 21+.

**Solution Applied:**
- Added JVM flag configuration: `quarkus.native.additional-build-args=--enable-native-access=ALL-UNNAMED`
- Created helper scripts to ensure the flag is set during local development

**Files Changed:**
- `iq-apis/src/main/resources/application.properties` - Added native access configuration
- `bin/run-apis-fixed` - Helper script with proper JVM flags

---

#### 4. **JWT Extractor Stub Warnings** (Expected in Development)
**Problem:**
```
WARNING: AuthGuardMiddleware is using a stub JWT extractor (no signature verification). 
This is only safe for development.
```

**Status:** ✓ NO CHANGE NEEDED - This is expected for development mode and is not a problem. 

**To Enable JWT Verification in Production:**
Set one of these environment variables:
- `MCP_JWT_SECRET` - For symmetric key validation
- `MCP_JWKS_URI` - For JWKS endpoint validation
- `MCP_OIDC_DISCOVERY_URL` - For OIDC discovery

---

## How to Use the Fixes

### Option 1: Run with Proper Configuration (Recommended for Development)

```bash
# Make the script executable
chmod +x bin/run-apis-fixed

# Run with proper JVM configuration
./bin/run-apis-fixed
```

### Option 2: Run with Maven directly (with JAVA_OPTS)

```bash
export JAVA_OPTS="--enable-native-access=ALL-UNNAMED -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
./mvnw -pl iq-apis -am quarkus:dev
```

### Option 3: Run pre-built JAR

```bash
export JAVA_OPTS="--enable-native-access=ALL-UNNAMED -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
java $JAVA_OPTS -jar iq-apis/target/iq-apis-0.94.1-runner.jar
```

---

## Verification

After applying these fixes, you should see:

✓ **No more JBoss LogManager initialization errors**
✓ **No more SLF4J multiple bindings warnings**  
✓ **No more ONNX Runtime native access warnings**
✓ **Clean startup with only INFO level and above logs**

The warnings about JWT are expected in development mode and can be safely ignored.

---

## Technical Details

### Logback Configuration (`logback.xml`)
- Configures console and file appenders
- Suppresses JBoss LogManager initialization warnings
- Enables development/test profile-specific logging levels
- Uses rolling file policy (10MB max, 5 backups)

### Dependency Exclusions
The quarkus-core dependency now explicitly excludes:
- `org.slf4j:slf4j-jboss-logmanager` - Old JBoss/SLF4J bridge
- `org.jboss.slf4j:jboss-slf4j` - JBoss SLF4J service provider

This leaves Logback as the sole SLF4J implementation, which is already configured in the root pom.xml.

### Native Access Configuration
The `--enable-native-access=ALL-UNNAMED` flag allows the ONNX Runtime library to use restricted methods without warnings. This is necessary for Java 21+ with ONNX 1.17.1.

---

## Troubleshooting

If you still see logging warnings after applying these changes:

1. **Clear Maven cache:**
   ```bash
   ./mvnw clean install -DskipTests
   ```

2. **Verify JAVA_HOME is set to JDK 21+:**
   ```bash
   java -version
   ```

3. **Check that logback.xml is in the classpath:**
   ```bash
   find . -name "logback.xml" -type f
   ```

4. **If running in Docker**, ensure the Dockerfile includes the JVM flag:
   ```dockerfile
   ENV JAVA_OPTS="--enable-native-access=ALL-UNNAMED"
   ```

---

## References

- [SLF4J Multiple Bindings](https://www.slf4j.org/codes.html#multiple_bindings)
- [Logback Configuration](https://logback.qos.ch/manual/configuration.html)
- [ONNX Runtime Java](https://github.com/microsoft/onnxruntime/tree/main/java)
- [Quarkus Logging Guide](https://quarkus.io/guides/logging)
- [Java 21 Native Access](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html)
