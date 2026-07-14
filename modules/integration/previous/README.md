# Previous agent builds (backward-compatibility tests)

This directory holds prebuilt older jvmguard agent releases that `PreviousAgentTest` runs against the
current server to verify an old agent still connects, reports data, and is shown as outdated.

It is currently empty because no old-agent versions are available, This means that `PreviousAgentTest` is skipped.

## Layout

One subdirectory per release, named by its version, mirroring an installed agent's `agent/` folder:

```
previous/
  <version>/
    jvmguard.jar
    lib/
      agent.jar
```

## Re-enabling the test

1. Add the version directory here as shown above.
2. Add the `<version>` string to `previousVersions` in `PreviousAgentTest.kt`. VM 1 runs the current agent;
   VMs 2..N run `previousVersions[vmNo - 2]`.
3. Record golden data (Java 8 only):
   ```
   ./gradlew :jvmguard:integration:integrationTest --tests "*PreviousAgentTest" -Pjdks=8 -Pjvmguard.record=true
   ```
   Copy the regenerated `PreviousAgentTest*.xml` from the test's work dir `output/` (under
   `build/gradle/jvmguard/integration/integration/PreviousAgentTest-jdk8/output/`) back into
   `../src/integrationTest/resources/dev/jvmguard/integration/tests/jvmguard/previous/data/`, then run
   `:jvmguard:integration:integrationTest --tests "*PreviousAgentTest" -Pjdks=8` (without the record flag)
   to confirm.
