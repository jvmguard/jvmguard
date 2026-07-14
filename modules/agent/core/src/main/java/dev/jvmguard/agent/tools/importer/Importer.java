package dev.jvmguard.agent.tools.importer;

import dev.jvmguard.agent.comm.CodecTypes;
import dev.jvmguard.agent.parameter.ConfigurationParameter;
import dev.jvmguard.agent.parameter.ConfigurationParameter.LogHandler;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Importer {
    public void importFile(File file, final File jvmguardUserDir) throws IOException {
        CodecTypes.registerAll();
        List<ConfigData> configs = new GroupConfigReader().read(file);

        Group root = new Group(null);
        for (ConfigData config : configs) {
            String[] hierarchyPath = config.getHierarchyPath().isEmpty() ? new String[0] : config.getHierarchyPath().split("/");
            root.addConfig(config, hierarchyPath, 0);
        }

        root.visit(group -> {
            group.deleteHierarchy(jvmguardUserDir);
            ConfigurationParameter.store(new ConfigurationParameter(group.getRecordingOptions(), group.getTransactionSettings(), group.getTelemetrySettings()),
                group.getConfigFile(jvmguardUserDir), new LogHandler() {
                    @Override
                    public void println(String s) {
                        System.out.println(s);
                    }

                    @Override
                    public void log(Throwable t) {
                        t.printStackTrace(System.out);
                    }
                });
        });
    }
}
