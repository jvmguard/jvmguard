package com.jvmguard.agent.jfr;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;

public class JfrRecorder {

    private static final long MAX_SIZE = 500 * 1000 * 1000;

    public static File record(String recordingName, int seconds, boolean predefined, String profileNameOrSettings) throws Exception {
        return Impl.record(recordingName, seconds, predefined, profileNameOrSettings);
    }

    @SuppressWarnings("Since15")
    private static class Impl {
        private static File record(String recordingName, int seconds, boolean predefined, String profileNameOrSettings) throws IOException, InterruptedException, ParseException {
            Configuration configuration = getConfiguration(predefined, profileNameOrSettings);
            Recording recording = new Recording(configuration);
            Path path = Files.createTempFile("jvmguard", ".jfr");
            try {
                recording.setMaxSize(MAX_SIZE);
                recording.setDestination(path);
                recording.setName(recordingName);
                recording.start();
                Thread.sleep(1000L * seconds);
                recording.stop();
            } finally {
                recording.close();
            }
            return path.toFile();
        }

        private static Configuration getConfiguration(boolean predefined, String profileNameOrSettings) throws IOException, ParseException {
            if (predefined) {
                return Configuration.getConfiguration("profile");
            } else {
                return Configuration.create(new StringReader(profileNameOrSettings));
            }
        }
    }
}
