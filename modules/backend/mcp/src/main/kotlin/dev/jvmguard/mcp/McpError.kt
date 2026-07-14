package dev.jvmguard.mcp

class McpError(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
