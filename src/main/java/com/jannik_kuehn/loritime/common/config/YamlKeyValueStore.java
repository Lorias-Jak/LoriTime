package com.jannik_kuehn.loritime.common.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YamlKeyValueStore implements KeyValueStore {
    private final Map<String, Object> data;
    private final String filePath;
    private boolean loaded;

    public YamlKeyValueStore(final String filePath) {
        this.data = new HashMap<>();
        this.filePath = filePath;
        loaded = false;

        loadFromFile();
    }

    @Override
    public void set(final String key, final Object value) {
        data.put(key, value);
        saveToFile();
    }

    @Override
    public Object get(String key) {
        return data.get(key);
    }

    @Override
    public Object get(final String key, final Object defaultValue) {
        Object obj = data.get(key);
        if (obj == null) {
            return defaultValue;
        }
        return obj;
    }

    @Override
    public List<String> getKeys() {
        return new ArrayList<>(data.keySet());
    }

    private void loadFromFile() {
        try {
            FileInputStream input = new FileInputStream(filePath);
            Yaml yaml = new Yaml();
            Map<String, Object> loadedData = yaml.load(input);
            data.putAll(loadedData);
            loaded = true;
        } catch (FileNotFoundException e) {
            loaded = false;
            e.printStackTrace();
        }
    }

    private void saveToFile() {
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);

        try (FileWriter writer = new FileWriter(filePath)) {
            yaml.dump(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isLoaded() {
        return loaded;
    }
}
