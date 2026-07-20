# jvmguard

**[jvmguard.dev](https://jvmguard.dev)** · [Documentation](https://jvmguard.dev/docs/main/introduction) · [Download](https://jvmguard.dev/download)

A JVM monitoring and profiling server with a built-in web UI. Connect JVMs via the
jvmguard agent, track telemetry and transactions, and capture deep profiles, either manually,
on threshold and policy triggers, or with a coding agent via the MCP server.
Every capture is access-controlled and audited.

<picture>
  <source srcset="https://jvmguard.dev/images/ui/profiling_options_dark.png" media="(prefers-color-scheme: dark)">
  <img src="https://jvmguard.dev/images/ui/profiling_options.png" alt="The jvmguard web UI showing a fleet of JVMs with live telemetry and the snapshot recording dialog.">
</picture>

## Get started

Download the installer for your platform from **[jvmguard.dev/download](https://jvmguard.dev/download)**,
or run from source:

```bash
git clone https://github.com/jvmguard/jvmguard.git
cd jvmguard
./gradlew :server:bootRun
```

The build auto-provisions a Java 25 toolchain. Once you see `Server started`, open
**http://localhost:8020/** and complete the setup. Use the "Add VMs" button in the
header of the web UI to start monitoring.

## Documentation

Full documentation, including installation, configuration, triggers, and the REST/MCP APIs,
lives at **[jvmguard.dev/docs](https://jvmguard.dev/docs/main/introduction)**.

## License

[Apache License 2.0](./LICENSE)
