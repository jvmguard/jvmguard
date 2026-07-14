package dev.jvmguard.agent.tools;

import dev.jvmguard.agent.tools.importer.Importer;

import java.io.File;

public class ToolsMain {
    @SuppressWarnings("ConfusingMainMethod")
    public static void main(String[] args, @SuppressWarnings("unused") File javaAgentJar, @SuppressWarnings("unused") File bootstrapBaseDir, File jvmguardUserDir) {
        if (args.length < 2 || !"import".equals(args[0])) {
            System.out.println("Usage: import <config.xml>");
            System.exit(1);
        }
        File file = new File(args[1]);
        if (!file.isFile()) {
            System.out.println("File " + file.getAbsolutePath() + " does not exist");
            System.exit(1);
        }
        System.out.println("Importing " + file.getAbsolutePath() + " ...");
        try {
            new Importer().importFile(file, jvmguardUserDir);
        } catch (Throwable e) {
            System.out.println("An error occurred during processing:");
            e.printStackTrace(System.out);
            System.exit(1);
        }
        System.exit(0);
    }
}
