package com.jvmguard.agent.tools.importer;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class GroupConfigReader {
    public List<ConfigData> read(File file) throws IOException {
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            root = JsonParser.object().from(reader);
        } catch (Exception e) {
            throw new IOException("Could not parse config file as JSON", e);
        }
        int version = root.getInt(ConfigFileFormat.KEY_VERSION, 0);
        if (version == 0) {
            throw new IOException("Missing version in config file");
        }
        if (version > ConfigFileFormat.FILE_VERSION) {
            throw new IOException("Config file version " + version + " is too new, supported version: " + ConfigFileFormat.FILE_VERSION);
        }
        List<ConfigData> configs = new ArrayList<>();
        JsonArray groups = root.getArray(ConfigFileFormat.KEY_GROUPS);
        if (groups != null) {
            for (Object element : groups) {
                try {
                    configs.add(new ConfigData((JsonObject)element));
                } catch (Exception e) {
                    throw new IOException("Could not read group config", e);
                }
            }
        }
        return configs;
    }
}
