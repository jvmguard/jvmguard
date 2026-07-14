package dev.jvmguard.agent.artifact;

import dev.jvmguard.agent.AgentInit;
import dev.jvmguard.agent.JvmGuardAgent;
import dev.jvmguard.agent.jprofiler.JProfilerArtifactHandler;

import java.io.File;

public final class ArtifactHandlers {

    private static final ArtifactHandler AGENT = new ArtifactHandler() {
        @Override
        public File getCacheBaseDir() {
            return JvmGuardAgent.getAgentUserDir();
        }

        @Override
        public File getInstallDir(String key) {
            return new File(getCacheBaseDir(), key);
        }

        @Override
        public boolean isReady(File installDir) {
            return new File(installDir, AgentInit.AGENT_JAR).isFile();
        }
    };

    private static final ArtifactHandler JPROFILER = new JProfilerArtifactHandler();

    private ArtifactHandlers() {
    }

    public static ArtifactHandler get(ArtifactKind kind) {
        switch (kind) {
            case AGENT:
                return AGENT;
            case JPROFILER:
                return JPROFILER;
            default:
                throw new IllegalArgumentException("Unknown artifact kind: " + kind);
        }
    }
}
