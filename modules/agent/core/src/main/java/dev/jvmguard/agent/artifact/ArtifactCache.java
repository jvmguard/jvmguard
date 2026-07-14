package dev.jvmguard.agent.artifact;

import dev.jvmguard.agent.util.JvmGuardUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ArtifactCache {

    public static void installAtomically(File installDir, File archive, ArtifactHandler handler) throws IOException {
        if (installDir.isDirectory() && handler.isReady(installDir)) {
            return;
        }
        if (installDir.exists()) {
            JvmGuardUtil.deleteDirectory(installDir);
        }
        File parent = installDir.getParentFile();
        parent.mkdirs();
        File tempDir = Files.createTempDirectory(parent.toPath(), "artifact").toFile();
        try {
            ArchiveExtractor.extract(archive, tempDir);
            if (!tempDir.renameTo(installDir)) {
                JvmGuardUtil.deleteDirectory(tempDir);
                // A concurrent installation may have produced a complete directory.
                if (!(installDir.isDirectory() && handler.isReady(installDir))) {
                    throw new IOException("Could not move extracted artifact into place: " + installDir);
                }
            }
        } catch (IOException e) {
            if (tempDir.isDirectory()) {
                JvmGuardUtil.deleteDirectory(tempDir);
            }
            throw e;
        }
    }
}
