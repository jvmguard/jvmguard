package com.jvmguard.agent.jprofiler;

import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.artifact.ArtifactHandler;

import java.io.File;

public class JProfilerArtifactHandler implements ArtifactHandler {

    @Override
    public File getCacheBaseDir() {
        return new File(JvmGuardAgent.getJvmGuardUserDir(), "jprofiler");
    }

    @Override
    public File getInstallDir(String key) {
        return new File(getCacheBaseDir(), key);
    }

    @Override
    public boolean isReady(File installDir) {
        return JProfilerLayout.resolveHome(installDir) != null;
    }
}
