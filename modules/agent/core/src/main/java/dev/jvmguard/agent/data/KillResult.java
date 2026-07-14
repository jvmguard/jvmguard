package dev.jvmguard.agent.data;

import dev.jvmguard.agent.comm.CommunicationContext;

import java.io.DataOutputStream;
import java.io.IOException;

public class KillResult extends BaseResult {
    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        System.out.println("exiting");
        System.exit(0);
    }
}
