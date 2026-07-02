package com.jvmguard.rest.resource

import com.jvmguard.common.JvmGuardDirectories
import com.jvmguard.data.user.RequireAdmin
import com.jvmguard.rest.restInterface.RestInterface
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class BackupResource(
    private val restInterface: RestInterface,
    private val directories: JvmGuardDirectories
) {

    @GetMapping(path = ["/triggerBackup"], produces = [MediaType.TEXT_PLAIN_VALUE])
    @RequireAdmin
    fun action(): String {
        restInterface.triggerBackup()
        return directories.backupDirectory.absolutePath
    }
}
