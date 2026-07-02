# Config serialization

Config persistence and the config-export file use **two independent JSON mechanisms**, each owning a
disjoint set of beans. They meet in exactly one place: the export file.

## The dividing line

| | **Server beans** | **Agent beans** |
|---|---|---|
| Examples | `User`, `GlobalConfig`, `GroupConfig`, the `*Set`s, `ServerGroupConfig`, `ServerConfig` | `AgentGroupConfig`, `RecordingOptions`, `TransactionSettings`/`TransactionDef`, `TelemetrySettings`/`MBeanTelemetryConfig`, `Policy`, `NamingElement` |
| Serialized by | **Jackson** | **the custom codec** |
| Base class | `StoredConfig` → `AbstractEntity` | `AbstractEntity` (directly, or via `OptionalConfig`) |
| Reach the agent? | never | yes (they are the agent's own config) |

The split exists because **the agent must stay dependency-free** — no Jackson inside monitored JVMs.
Server beans never cross to the agent, so they are free to use Jackson. The line is the module boundary.

## Jackson (server beans) — `ConfigStorage.objectMapper()`

One shared `ObjectMapper`, configured three ways to handle these JavaBeans:

- **Field-based visibility** (`GETTER`/`IS_GETTER` = `NONE`, `FIELD` = `ANY`): the beans carry *computed*
  getters (`VmIdentifier.getUnqualifiedPath()` is self-referential; `ThresholdTrigger.getDescription()`
  NPEs on a null sub-field) that must not be persisted. Field access persists actual state and
  ignores those.
- **Default typing `NON_FINAL`**: the stored graphs are polymorphic (`TransactionDefSet` holds
  `TransactionDef` subtypes, etc.); type info is embedded so deserialization resolves the concrete subtype.
- **A mixin on `AbstractEntity`** ignoring `modified` / `changeListeners` (transient runtime state).

Used in two places: **DB storage** (`config_storage.content` is Jackson JSON) and the **`serverConfig`
part of the export file**.

## The custom codec (agent beans) — one definition, two backends

Agent beans implement `CodecBean`:

```java
interface CodecBean {
    String codecType();                  // stable polymorphism discriminator, both backends
    void readState(AgentReader r);
    void writeState(AgentWriter w);
}
```

`AgentReader` / `AgentWriter` are **format-agnostic** — field-level methods that each take a field name:

```java
r.readString("retransformationType");   w.writeEnum("retransformationType", retransformationType);
r.readObject("policy");                 w.writeObject("policy", policy);    // polymorphic
r.readList("transactionDefs", list);    w.writeList("transactionDefs", list);
```

The bean is **blind to the format**. Two backends implement those interfaces:

- **`BinaryAgentReader/Writer`** — *positional*: wraps `DataInputStream`/`DataOutputStream`, ignores the
  field names, reads/writes in call order. **This is the live agent↔server wire protocol.**
- **`JsonAgentReader/Writer`** — *named*: backed by **nanojson**; field names become JSON keys, nested
  beans carry a `@type` discriminator. This is the `agentConfig` part of the export file.

Polymorphism on read (`readObject`) goes through `CodecRegistry` (keyed by `codecType()`, populated by
`CodecTypes.registerAll()`, called at both agent and server startup).

**The key property:** each agent bean has **one** `readState`/`writeState`. That single definition
produces *both* the live binary protocol bytes *and* the JSON export — no duplication. The caller just
hands it a `BinaryAgent*` or a `JsonAgent*`.

## Where they meet: the export file

`jvmguard_server_config.json` / `jvmguard_recording_config.json` is one JSON file assembled from three
sources — a nanojson envelope, codec-produced subtrees, and Jackson-produced subtrees:

```json
{
  "version": 2, "type": "serverInit",                     ← envelope (nanojson)
  "groups": [{
    "path": "...", "groupType": 0,
    "agentConfig": {                                          ← CODEC (JsonAgentWriter)
      "recordingOptions": {"@type":"RecordingOptions","retransformationType":"STARTUP"},
      "transactionSettings": { … },
      "telemetrySettings": { … } },
    "serverConfig": ["com.jvmguard…ServerGroupConfig", { … }]   ← JACKSON (type-tagged array)
  }],
  "serverConfig": ["com.jvmguard…ServerConfig", { … }]    ← JACKSON (global/users/sets)
}
```

- The **envelope** (`version`, `type`, `groups`, `path`, `groupType`) is plain JSON.
- `groups[].agentConfig` is built by handing the agent bean to `JsonAgentWriter` → a nanojson object.
- `groups[].serverConfig` (per-group) and the top-level `serverConfig` are built by
  `ConfigStorage.objectMapper().writeValueAsString(...)` → a Jackson type-tagged array, embedded verbatim.

nanojson is the **container**; the codec and Jackson each produce JSON fragments that nanojson holds.

## Who consumes what

- **The agent offline importer** reads **only `groups[].agentConfig`** via the codec (`JsonAgentReader`).
  It ignores both `serverConfig` fields — so the agent needs only **nanojson + the codec**, no Jackson.
- **The server re-import** reads everything: `agentConfig` through the codec, both `serverConfig` parts
  through Jackson.

## Key classes

| Concern | Location |
|---|---|
| Jackson mapper (shared, server) | `com.jvmguard.common.config.ConfigStorage#objectMapper()` |
| Storage (server beans → DB JSON) | `com.jvmguard.common.config.ConfigStorage` |
| Codec interfaces + backends | `com.jvmguard.agent.comm` (`CodecBean`, `AgentReader`/`AgentWriter`, `BinaryAgent*`, `JsonAgent*`) |
| Codec type registry | `com.jvmguard.agent.comm.CodecRegistry`, `CodecTypes.registerAll()` |
| Export/import format | `com.jvmguard.data.config.external` (`ServerInitConfig`, `RecordingConfig`, `ServerConfig`) |
| Format constants | `com.jvmguard.agent.tools.importer.ConfigFileFormat` |
| Agent offline importer | `com.jvmguard.agent.tools.importer` (`GroupConfigReader`, `ConfigData`, `Importer`) |
| Bean base classes | `com.jvmguard.agent.config.base` (`AbstractEntity`, `Identifiable`, `OptionalConfig`) |
| Shadowed JSON lib (agent) | `com.grack:nanojson`, relocated to `com.jvmguard.agent.json` in the agent jar |
