package com.jvmguard.mcp

object McpInstructions {
    const val WORKFLOW_GUIDE = """
jvmguard monitors JVMs in production. Typical workflow:

1. Call list_groups and list_vms to discover the monitored fleet.
2. Call list_telemetries to see which telemetries a VM records.
3. Call get_telemetry to inspect time-series data (CPU, heap, GC, threads, etc.).
4. Call get_call_tree or get_hotspots to analyze transaction performance, and
   get_overdue_transactions to find transactions over their thresholds.
5. If you find issues, use heap_dump, thread_dump, record_jfr, or record_jps
   to capture diagnostic artifacts (requires profiler access). Captures appear in
   list_snapshot_files when ready, and get_snapshot_file retrieves one.
6. Use list_mbeans and get_mbean_data to inspect MBeans, list_log_files and
   get_log_file to read logs, and get_inbox for trigger notifications.

VMs, pools, and groups are referenced by their hierarchy path (e.g. "Demo/Storefront").
The root group is the empty string "". A pool (kind "pool" in list_vms) aggregates several
identical VMs under one path, so reads such as get_telemetry work on the pool path; its live
members appear as kind "instance" when you call list_vms with connected=true.
Telemetry intervals: 10min, 20min, 40min, 80min, 3h, 6h, 12h, 1d, 3d, 6d, 12d, 30d, 60d, 180d.
Transaction tree intervals: 1min, 10min, 1h, 1d.
"""
}
