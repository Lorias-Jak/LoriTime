package com.jannik_kuehn.common.config;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("PMD.CommentRequired")
public class YamlKeyValueStore implements KeyValueStore {
    private final String filePath;

    private final LoriTimeLogger log;

    private final Map<String, Object> data;

    private boolean loaded;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public YamlKeyValueStore(final String filePath) {
        this.filePath = filePath;
        final LoriTimePlugin loriTimePlugin = LoriTimePlugin.getInstance();
        this.log = loriTimePlugin.getLoggerFactory().create(YamlKeyValueStore.class);
        this.data = new HashMap<>();
        loaded = false;

        loadFromFile();
    }

    @Override
    public void set(final String key, final Object value) {
        data.put(key, value);
        saveToFile();
    }

    @Override
    public void setTemporary(final String key, final Object value) {
        data.put(key, value);
    }

    @Override
    public Object get(final String key) {
        return data.get(key);
    }

    @Override
    public Object get(final String key, final Object defaultValue) {
        final Object obj = data.get(key);
        if (obj == null) {
            return defaultValue;
        }
        return obj;
    }

    @Override
    public Map<String, ?> getAll() {
        return data;
    }

    public void setAll(final Map<String, ?> dataMap) {
        data.putAll(dataMap);
        saveToFile();
    }

    @Override
    public List<String> getKeys() {
        return new ArrayList<>(data.keySet());
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return data.entrySet();
    }

    @SuppressWarnings({"PMD.AvoidFileStream", "PMD.UnusedAssignment"})
    private void loadFromFile() {
        try (FileInputStream input = new FileInputStream(filePath)) {  // try-with-resources Block
            final Yaml yaml = new Yaml();
            final Map<String, Object> loadedData = yaml.load(input);
            if (loadedData == null) {
                loaded = true;
                log.warn("Your file '" + filePath + "' seems to be empty. Is this right?");
                return;
            }
            data.clear();
            final Map<String, ?> test = readRecursive(loadedData, "");
            data.putAll(test);
            loaded = true;
        } catch (final FileNotFoundException e) {
            loaded = false;
            log.error("Failed to load data from file: " + filePath, e);
        } catch (final IOException e) {
            loaded = false;
            log.error("IO Exception while loading data from file: " + filePath, e);
        }
    }

    private Map<String, ?> readRecursive(final Map<String, ?> map, final String keyChain) {
        final String subPathPrefix = keyChain == null || keyChain.isEmpty() ? "" : keyChain + ".";
        final Map<String, Object> dataMap = new HashMap<>();
        for (final Map.Entry<String, ?> entry : map.entrySet()) {
            final String key = entry.getKey();
            final Object data = entry.getValue();
            if (data instanceof LinkedHashMap<?, ?>) {
                dataMap.putAll(readRecursive((Map<String, ?>) data, subPathPrefix + key));
            } else {
                dataMap.put(subPathPrefix + key, data);
            }
        }
        return dataMap;
    }

    @SuppressWarnings("PMD.AvoidFileStream")
    private void saveToFile() {
        final DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        final Yaml yaml = new Yaml(options);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            yaml.dump(data, writer);
        } catch (final IOException e) {
            log.error("Failed to save data to file: " + filePath, e);
        }
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void reload() {
        loadFromFile();
    }

    @Override
    public void remove(final String key) {
        data.remove(key);
    }
}
