# Contributing to jvmguard

Contributions are welcome under the [Apache License 2.0](./LICENSE).

## Running the tests

The root project defines an `allTests` task that aggregates every test in the project (backend and
UI unit tests, the UI browser tests, and the agent integration tests):

```bash
./gradlew allTests
```

For most changes you can run the relevant module tests directly instead, for example:

```bash
./gradlew :ui:test
```

### Agent and backend changes

Changes to the **agent** (`modules/agent`) or the **backend** (`modules/backend`, `modules/server`)
must be verified with the agent integration tests, which launch a real server plus
agent-instrumented workload JVMs and assert against golden data:

```bash
./gradlew :integration:integrationTest
```

By default this runs against the JDK running the build and takes more than 2 hours. The full JDK matrix
(`-Pjdks=all`) takes much longer. To run just one test with the current
JDK, execute:

```bash
./gradlew :integration:integrationTest --tests "*<TestName>"
```

See [AGENTS.md](./AGENTS.md) for the integration test layout, re-recording golden data, and the full
development guide.
