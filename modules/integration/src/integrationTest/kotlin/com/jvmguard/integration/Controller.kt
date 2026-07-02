package com.jvmguard.integration

import java.io.File

/**
 * Controller for tests
 */
interface Controller {

    /** Golden data is written to this directory in record mode. */
    val workingDir: File

    /** Signals that all assertions are done. */
    fun finished()
}
