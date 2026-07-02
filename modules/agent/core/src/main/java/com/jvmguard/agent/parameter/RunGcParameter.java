package com.jvmguard.agent.parameter;

import com.jvmguard.agent.comm.CommunicationContext;

import java.io.DataInputStream;
import java.io.IOException;

public class RunGcParameter extends BaseParameter {
    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        System.gc();
    }
}
