package com.jvmguard.agent.data;

import java.io.File;
import java.io.IOException;

public interface FileMover {
    void moveToFile(File file) throws IOException;
    long getUncompressedLength();
}
