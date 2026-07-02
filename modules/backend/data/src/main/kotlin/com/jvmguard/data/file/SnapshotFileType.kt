package com.jvmguard.data.file

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

enum class SnapshotFileType(
    private val verbose: String,
    val extension: String,
    val databaseId: Int,
) {
    HPZ("Memory snapshot", "hpz", 1),
    JPS("CPU snapshot", "jps", 2),
    THREAD_DUMP("Thread dump", "txt", 3),
    JFR("JFR snapshot", "jfr", 4);

    override fun toString(): String = verbose

    companion object {
        private val databaseIdToSnapshotFileType = Int2ObjectOpenHashMap<SnapshotFileType>().apply {
            for (snapshotFileType in SnapshotFileType.entries) {
                put(snapshotFileType.databaseId, snapshotFileType)
            }
        }

        fun fromDatabaseId(databaseId: Int): SnapshotFileType? = databaseIdToSnapshotFileType[databaseId]
    }
}
