package dev.jvmguard.common.util

import java.io.File
import java.net.URL

class LoadingDescriptor private constructor(
    val baseDir: File,
    val classpathDir: File,
    val isLoadedFromJAR: Boolean,
) {

    override fun toString() =
        "base dir: $baseDir, class path dir: $classpathDir, loadedFromJar: $isLoadedFromJAR"

    companion object {
        const val URL_FILE_PREFIX = "file:"
        const val URL_JAR_PREFIX = "jar:file:"

        fun getInstance(clazz: Class<*>): LoadingDescriptor {
            val className = clazz.name.replace('.', '/') + ".class"
            val classLocation = getResource(clazz, className).toString()

            var baseName = classLocation.substring(0, classLocation.lastIndexOf(className) - 1)
            baseName = decodePath(baseName)
            val classPathName: String
            val loadedFromJAR: Boolean
            when {
                baseName.startsWith(URL_JAR_PREFIX) -> {
                    loadedFromJAR = true
                    classPathName = baseName.substring(URL_JAR_PREFIX.length, baseName.length - 1)
                    baseName = getJarBaseName(baseName)
                }

                baseName.startsWith(URL_FILE_PREFIX) -> {
                    loadedFromJAR = false
                    baseName = baseName.substring(URL_FILE_PREFIX.length)
                    classPathName = baseName
                }

                else -> {
                    throw RuntimeException("Base URL $baseName is invalid")
                }
            }

            return LoadingDescriptor(File(baseName), File(classPathName), loadedFromJAR)
        }

        fun decodePath(path: String): String {
            val buffer = StringBuilder()
            var c: Char
            var i = 0
            while (i < path.length) {
                c = path[i]
                if (c != '%') {
                    i++
                    buffer.append(c)
                    continue
                }
                try {
                    c = unescape(path, i)
                    i += 3
                    if (c.code and 128 != 0) {
                        when (c.code shr 4) {
                            12, 13 -> {
                                val c1 = unescape(path, i)
                                i += 3
                                c = ((c.code and 31 shl 6) or (c1.code and 63)).toChar()
                            }

                            14 -> {
                                val c2 = unescape(path, i)
                                i += 3
                                val c3 = unescape(path, i)
                                i += 3
                                c = ((c.code and 15 shl 12) or (c2.code and 63 shl 6) or (c3.code and 63)).toChar()
                            }

                            else -> throw IllegalArgumentException()
                        }
                    }
                } catch (_: NumberFormatException) {
                    throw IllegalArgumentException()
                }
                buffer.append(c)
            }

            return buffer.toString()
        }

        private fun unescape(s: String, i: Int): Char =
            s.substring(i + 1, i + 3).toInt(16).toChar()

        private fun getJarBaseName(baseName: String): String {
            var lastIndex = baseName.lastIndexOf('/')
            if (lastIndex == -1) {
                lastIndex = baseName.lastIndexOf('\\')
            }
            if (lastIndex == -1) {
                throw RuntimeException("Base URL $baseName is invalid")
            }
            return baseName.substring(URL_JAR_PREFIX.length, lastIndex)
        }

        private fun getResource(clazz: Class<*>, className: String): URL {
            val simpleName = className.substring(className.lastIndexOf('/') + 1)
            return clazz.getResource(simpleName)
                ?: throw RuntimeException("Class $className has no resource")
        }
    }
}
