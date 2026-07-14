package dev.jvmguard.agent.data;

import dev.jvmguard.agent.util.MutableInt;
import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.instrument.Transformer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PackageStats extends BaseResult {
    private Map<String, Integer> packageToValue;

    public Map<String, Integer> getPackageToValue() {
        return packageToValue;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        packageToValue = new HashMap<>();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            String packageName = in.readUTF();
            Integer value = in.readInt();
            packageToValue.put(packageName.replace('/', '.'), value);
        }
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        Map<String, MutableInt> map = Transformer.getInstance().getPackageStats();
        out.writeInt(map.size());
        for (Entry<String, MutableInt> entry : map.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt((entry.getValue()).val);
        }
    }
}
