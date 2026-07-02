# jvmguard

jvmguard is a production JVM-profiling **server**: a single, self-contained application that
continuously collects telemetry, transactions, call trees and live profiling data from the JVMs
you monitor with the jvmguard agent, and presents it through a built-in web UI.

It is licensed under the [Apache License 2.0](./LICENSE).

## Running from source

There are no prebuilt downloads yet, so jvmguard is run directly from source. To run the current
development version:

```bash
git clone https://github.com/jvmguard/jvmguard.git jvmguard
cd jvmguard
./gradlew :server:bootRun
```

This builds the UI and launches the server. No manual Java setup is needed, the build
auto-provisions a Java 25 toolchain. Once you see `Server started`, open **http://localhost:8020/** 
in your browser.

On first launch, complete the setup wizard to create your admin login, then start monitoring
JVMs by pointing them at the jvmguard agent port (default `8847`).

The default web port is `8020` and the agent port is `8847`. Both can be changed, see
[Configuration](#configuration).

> The build and test layout, the full development workflow, the demo cluster, the `?mock` data mode
> and all contributor conventions are documented in [AGENTS.md](./AGENTS.md).

## Configuration

The built-in defaults are shipped in
[`application.yaml`](modules/server/src/main/resources/application.yaml). To override values, place
an `application.yaml` in one of these locations (later entries are not read once an earlier one
exists):

- `<install-dir>/config/application.yaml` (recommended), or
- `<install-dir>/application.yaml`, or
- `<install-dir>/jvmguard.properties` (a flat `key=value` format).

Any individual key can also be set with a `-Djvmguard.<key>=<value>` system property, which always
takes precedence.

## Documentation

The product documentation lives in [`modules/docs`](./modules/docs) (an Astro Starlight site). See
[AGENTS.md](./AGENTS.md) for how to build and serve it locally.

## License

Apache License 2.0, see [LICENSE](./LICENSE) and [NOTICE](./NOTICE).
