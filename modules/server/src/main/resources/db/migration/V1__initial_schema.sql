-- JvmGuard database (jvmguard.mv.db) fixed-schema tables. Fresh installs only; H2 2.x (MVStore) is the only dialect.
--
-- NOT created here (kept programmatic, by design — created lazily by the storage beans in @PostConstruct):
--   * telemetry_data_<sec>        -- one partition per TelemetryDataInterval, created lazily
--   * <type>_<interval>            -- transaction blob partitions (TransactionDataType x interval)
--   * <prefix>_names / <prefix>_caps  -- name-interning for the storage managers

CREATE TABLE snapshot_file (
    id                 BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    vmId               BIGINT NOT NULL,
    type               INT    NOT NULL,
    snapshotTime       BIGINT NOT NULL,
    name               VARCHAR(20000),
    uncompressedLength BIGINT
);

CREATE TABLE inbox (
    id               BIGINT     NOT NULL AUTO_INCREMENT PRIMARY KEY,
    userId           BIGINT     NOT NULL,
    inboxTime        BIGINT     NOT NULL,
    snapshotFileId   BIGINT     NOT NULL,
    snapshotFileType INT        NOT NULL,
    vmId             BIGINT     NOT NULL,
    name             TEXT,
    message          MEDIUMTEXT,
    itemRead         BOOL       NOT NULL
);
CREATE INDEX inbox_user ON inbox (userId);
CREATE INDEX inbox_snapshot ON inbox (snapshotFileId);
CREATE INDEX inbox_vm ON inbox (vmId);

CREATE TABLE transaction_consolidation (
    type              VARCHAR(255) PRIMARY KEY,
    consolidationTime BIGINT NOT NULL
);

-- Unified document store for every StoredConfig subclass (User, GlobalConfig, GroupConfig and the
-- five *Set beans). 'id' is a single global auto-increment key; it is globally unique, hence trivially
-- unique within a bean_type, which preserves the old per-type id contract.
CREATE TABLE config_storage (
    id        BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    bean_type VARCHAR(255) NOT NULL,
    content   MEDIUMTEXT   NOT NULL
);
CREATE INDEX idx_config_storage_type ON config_storage (bean_type);

-- VM registry: vm is the aggregate root, vm_instance a one-to-one referenced entity (vmId back-ref).
CREATE TABLE vm (
    id          BIGINT  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    vmType      TINYINT NOT NULL,
    nameId      INT     NOT NULL,
    groupNameId INT     NOT NULL,
    instanceId  BIGINT  NOT NULL
);
CREATE TABLE vm_instance (
    instanceId BIGINT NOT NULL PRIMARY KEY,
    vmId       BIGINT NOT NULL,
    hostNameId INT    NOT NULL,
    port       INT    NOT NULL
);

CREATE TABLE update_check (
    newVersion VARCHAR(100) PRIMARY KEY
);

CREATE TABLE additional_telemetry (
    id          INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type        INT           NOT NULL,
    name        VARCHAR(3000) NOT NULL,
    description VARCHAR(3000) NOT NULL,
    hidden      BOOL
);
CREATE TABLE additional_telemetry_format (
    id           INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type         INT           NOT NULL,
    nodeName     VARCHAR(3000) NOT NULL,
    unit         INT           NOT NULL,
    scale        INT           NOT NULL,
    stacked      BOOL          NOT NULL,
    groupAverage BOOL          NOT NULL
);
-- (type, nodeName) is the logical key the MERGE upsert in AdditionalTelemetryManager.updateFormat keys on.
CREATE UNIQUE INDEX uq_additional_telemetry_format ON additional_telemetry_format (type, nodeName);

CREATE TABLE telemetry_list_ids (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    version INT,
    content MEDIUMBLOB
);
