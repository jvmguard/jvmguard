package com.jvmguard.agent.artifact;

import java.io.File;

public interface ArtifactHandler {

    File getCacheBaseDir();
    File getInstallDir(String key);
    boolean isReady(File installDir);
}
