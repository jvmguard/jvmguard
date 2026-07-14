package dev.jvmguard.agent.config.transactions.naming;

import dev.jvmguard.agent.comm.*;
import dev.jvmguard.agent.config.base.ConfigDoc;
import dev.jvmguard.agent.config.base.DefaultConstructor;
import dev.jvmguard.agent.config.transactions.EnvironmentException;
import dev.jvmguard.agent.config.transactions.NamingElement;

import java.io.DataInputStream;
import java.io.DataOutputStream;

@ConfigDoc("Adds the declared class name (optionally with package) as a name segment.")
public class ClassNameElement extends NamingElement {

    @ConfigDoc("How much of the package to include in the class-name segment.")
    private PackageMode packageMode = PackageMode.NONE;

    public PackageMode getPackageMode() {
        return packageMode;
    }

    @DefaultConstructor
    public ClassNameElement() {
    }

    public ClassNameElement(PackageMode packageMode) {
        this.packageMode = packageMode;
    }

    public void setPackageMode(PackageMode packageMode) {
        PackageMode oldValue = this.packageMode;
        this.packageMode = packageMode;
        fireChanged(oldValue, packageMode);
    }

    @Override
    public boolean isIdentical(NamingElement namingElement) {
        if (!super.isIdentical(namingElement)) {
            return false;
        }
        ClassNameElement other = (ClassNameElement)namingElement;
        return packageMode == other.packageMode;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws Exception {
        readState(new BinaryAgentReader(in));
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws Exception {
        writeState(new BinaryAgentWriter(out));
    }

    @Override
    public String codecType() {
        return "ClassNameElement";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        packageMode = reader.readEnum("packageMode", PackageMode.class);
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeEnum("packageMode", packageMode);
    }

    @Override
    public String getDisplayName() {
        return "Class name " + packageMode.toString().toLowerCase();
    }

    @Override
    public boolean canBeStatic() {
        return true;
    }

    public void appendName(StringBuilder buffer, TransactionEnvironment environment) throws EnvironmentException {
        String className = getClassName(environment);
        if (packageMode == PackageMode.FULL) {
            buffer.append(className);
        } else {
            int dotIndex = className.lastIndexOf('.');
            if (dotIndex < 0) {
                buffer.append(className);
            } else {
                switch (packageMode) {
                    case NONE:
                        buffer.append(className, dotIndex + 1, className.length());
                        break;
                    case ABBREVIATED:
                        boolean appendNext = true;
                        for (int i = 0; i < dotIndex; i++) {
                            char c = className.charAt(i);
                            if (c == '.') {
                                buffer.append('.');
                                appendNext = true;
                            } else if (appendNext) {
                                buffer.append(c);
                                appendNext = false;
                            }
                        }
                        buffer.append(className, dotIndex, className.length());
                        break;
                }
            }
        }
    }

    protected String getClassName(TransactionEnvironment environment) {
        return environment.getClassName();
    }

    public interface TransactionEnvironment {
        String getClassName();
    }

    // do not rename enums
    public enum PackageMode {
        @ConfigDoc("Class name without package.")
        NONE("Without package name"),
        @ConfigDoc("Abbreviated package (e.g. c.e.g.Type).")
        ABBREVIATED("With abbreviated package name (e.g. \"c.e.g.t\")"),
        @ConfigDoc("Fully-qualified class name.")
        FULL("With full package name");

        private final String verbose;

        PackageMode(String verbose) {
            this.verbose = verbose;
        }

        @Override
        public String toString() {
            return verbose;
        }
    }
}
