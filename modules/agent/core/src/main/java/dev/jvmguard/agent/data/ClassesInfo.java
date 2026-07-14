package dev.jvmguard.agent.data;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.instrument.Instrumenter;
import dev.jvmguard.agent.instrument.Transformer;
import dev.jvmguard.agent.parameter.ClassesInfoParameter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClassesInfo extends BaseResult {
    private List<String> classNames;

    public ClassesInfo() {
    }

    public List<String> getClassNames() {
        return classNames;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        classNames = new ArrayList<>();
        while (in.readBoolean()) {
            String classNameName = in.readUTF();
            classNames.add(classNameName);
        }
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        boolean allClasses = ((ClassesInfoParameter)context.getProperty(CommunicationContext.PROPERTY_PARAMETER)).isAllClasses();

        Transformer transformer = Transformer.getInstance();
        Class[] classes = transformer.getAllLoadedClasses();
        for (Class clazz : classes) {
            if (!clazz.isArray() && !clazz.isPrimitive() && !clazz.getName().startsWith("__jvmguard")) {
                if (allClasses || (clazz.getClassLoader() != null && transformer.getInstrumenter().isHardFiltered(clazz.getName().replace('.', '/')) == Instrumenter.HARD_FILTERED_UNFILTERED)) {
                    out.writeBoolean(true);
                    out.writeUTF(clazz.getName());
                }
            }
        }
        out.writeBoolean(false);
    }
}
