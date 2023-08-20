package com.jannik_kuehn.loritime.common.config;

import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    public void setAll(final Map<String, ?> dataMap) {
        data.putAll(dataMap);
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
    public Map<String, ?> getAll() {
        return data;
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
            if (loadedData == null) {
                loaded = true;
                LoriTimePlugin.getInstance().getLogger().warning("Your file '" + filePath + "' seems to be empty. Is this right?");
                return;
            }
            data.clear();
            Map<String, ?> test= readRecursive(loadedData, "");
            data.putAll(test);
            loaded = true;
        } catch (FileNotFoundException e) {
            loaded = false;
            e.printStackTrace();
        }
    }

    private Map<String, ?> readRecursive(Map<String, ?> map, String keyChain) {
        String subPathPrefix = keyChain == null || keyChain.isEmpty() ? "" : keyChain + ".";
        Map<String, Object> dataMap = new HashMap<>();
        for (String key : map.keySet()) {
            Object data = map.get(key);
            if (data instanceof LinkedHashMap<?,?>) {
                dataMap.putAll(readRecursive((Map<String, ?>) data, subPathPrefix + key));
            } else if (data instanceof ArrayList<?>) {
                dataMap.put(subPathPrefix + key,data);
            } else {
                dataMap.put(subPathPrefix + key, data);
            }
        }
        return dataMap;
    }

    private void saveToFile() {
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

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

    public void reload() {
        loadFromFile();
    }
}
