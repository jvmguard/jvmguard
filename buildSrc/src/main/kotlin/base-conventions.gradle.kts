import com.jvmguard.build.rootBuildDir
import java.io.File

// Relocate every module's build directory into one central tree under <root>/build/gradle/<path>.
layout.buildDirectory.set(File(rootBuildDir, path.substring(1).replace(':', '/')))
