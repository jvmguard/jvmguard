-- Indexes that were previously created programmatically by VmStorage/SnapshotFileStorage @PostConstruct.

CREATE INDEX IF NOT EXISTS vm_query ON vm (groupNameId, vmType, nameId);
CREATE INDEX IF NOT EXISTS snapshot_file_query ON snapshot_file (vmId, type);
CREATE INDEX IF NOT EXISTS snapshot_file_type ON snapshot_file (type);
