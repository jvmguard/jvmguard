package com.jvmguard.agent.parameter;

import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class JfrRecordParameters extends BaseParameter {
    private String recordingName;
    private int seconds;
    private boolean predefined;
    private String profileNameOrSettings;

    @DefaultConstructor
    public JfrRecordParameters() {
    }

    public JfrRecordParameters(String recordingName, int seconds, boolean predefined, String profileNameOrSettings) {
        this.recordingName = recordingName;
        this.seconds = seconds;
        this.predefined = predefined;
        this.profileNameOrSettings = profileNameOrSettings;
    }

    public String getRecordingName() {
        return recordingName;
    }

    public int getSeconds() {
        return seconds;
    }

    public boolean isPredefined() {
        return predefined;
    }

    public String getProfileNameOrSettings() {
        return profileNameOrSettings;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeUTF(recordingName);
        out.writeInt(seconds);
        out.writeBoolean(predefined);
        out.writeUTF(profileNameOrSettings);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        recordingName = in.readUTF();
        seconds = in.readInt();
        predefined = in.readBoolean();
        profileNameOrSettings = in.readUTF();
    }
}
