# Previous agent builds (backward-compatibility tests)

This directory holds prebuilt older jvmguard agent releases that `PreviousAgentTest` runs against the
current server to verify an old agent still connects, reports data, and is shown as outdated.

## Layout

One subdirectory per release, named by its version, mirroring an installed agent's `agent/` folder:

```
previous/
  <version>/
    jvmguard.jar
    lib/
      agent.jar
```

## Adding a version when the protocol version changes

1. Add the version directory here as shown above.
2. Add the `<version>` string to `previousVersions` in `PreviousAgentTest.kt`. VM 1 runs the current agent;
   VMs 2..N run `previousVersions[vmNo - 2]`.
3. Record golden data (Java 8 only):
   ```
   ./gradlew :integration:integrationTest --tests "*PreviousAgentTest" -Pjdks=8 -Pjvmguard.record=true
   ```
   Copy the regenerated `PreviousAgentTest*.xml` from the test's work dir `output/` (under
   `build/gradle/integration/integration/PreviousAgentTest-jdk8/output/`) back into
   `../src/integrationTest/resources/dev/jvmguard/integration/tests/jvmguard/previous/data/`, then run
   `:integration:integrationTest --tests "*PreviousAgentTest" -Pjdks=8` (without the record flag)
   to confirm.
