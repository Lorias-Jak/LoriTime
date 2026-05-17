package com.jannik_kuehn.common.config;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * YAML-backed configuration implementation using a structured document model.
 */
public class YamlConfiguration extends Configuration {
    /**
     * Backing file path.
     */
    private final String filePath;

    /**
     * Logger for configuration diagnostics.
     */
    private final WrappedLogger log;

    /**
     * Structured document contents.
     */
    private StructuredConfigurationDocument document;

    /**
     * True when the backing file loaded successfully.
     */
    private boolean loaded;

    /**
     * Creates a YAML configuration backed by the given file.
     *
     * @param name          file path
     * @param loggerFactory logger factory for diagnostics
     */
    public YamlConfiguration(final String name, final LoggerFactory loggerFactory) {
        super(null);
        this.filePath = name;
        this.log = loggerFactory.create(YamlConfiguration.class);
        this.document = new StructuredConfigurationDocument();
        loadFromFile();
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(final String key, final Object value) {
        document.set(key, value);
        saveToFile();
    }

    /** {@inheritDoc} */
    @Override
    public void setTemporaryValue(final String key, final Object value) {
        document.set(key, value);
    }

    /** {@inheritDoc} */
    @Override
    public String getString(final String key) {
        return getRootSection().getString(key);
    }

    /** {@inheritDoc} */
    @Override
    public String getString(final String key, final String def) {
        final String value = getString(key);
        if (value == null) {
            logInvalidFallback(key, "string", document.get(key), def);
            return def;
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public int getInt(final String key) {
        return getRootSection().getInt(key);
    }

    /** {@inheritDoc} */
    @Override
    public int getInt(final String key, final int def) {
        final Object value = document.get(key);
        if (value instanceof final Number number) {
            return number.intValue();
        }
        logInvalidFallback(key, "number", value, def);
        return def;
    }

    /** {@inheritDoc} */
    @Override
    public long getLong(final String key) {
        return getRootSection().getLong(key);
    }

    /** {@inheritDoc} */
    @Override
    public long getLong(final String key, final long def) {
        final Object value = document.get(key);
        if (value instanceof final Number number) {
            return number.longValue();
        }
        logInvalidFallback(key, "number", value, def);
        return def;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getBoolean(final String key) {
        return getRootSection().getBoolean(key);
    }

    /** {@inheritDoc} */
    @Override
    public boolean getBoolean(final String key, final boolean def) {
        final Object value = document.get(key);
        if (value instanceof final Boolean bool) {
            return bool;
        }
        logInvalidFallback(key, "boolean", value, def);
        return def;
    }

    /** {@inheritDoc} */
    @Override
    public List<?> getArrayList(final String key) {
        return getRootSection().getArrayList(key);
    }

    /** {@inheritDoc} */
    @Override
    public Object getObject(final String key) {
        return document.get(key);
    }

    /** {@inheritDoc} */
    @Override
    public Object getObject(final String key, final Object def) {
        return getRootSection().getObject(key, def);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getKeys() {
        return List.copyOf(document.flatten().keySet());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isLoaded() {
        return loaded;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> getAll() {
        return document.flatten();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> getStructuredValues() {
        return document.asMap();
    }

    /** {@inheritDoc} */
    @Override
    public ConfigSection getRootSection() {
        return document.rootSection();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<ConfigSection> getSection(final String key) {
        return document.getSection(key);
    }

    /** {@inheritDoc} */
    @Override
    public void reload() {
        loadFromFile();
    }

    /** {@inheritDoc} */
    @Override
    public void remove(final String key) {
        document.remove(key);
        saveToFile();
    }

    @SuppressWarnings({"PMD.AvoidFileStream", "PMD.UnusedAssignment"})
    private void loadFromFile() {
        try (FileInputStream input = new FileInputStream(filePath)) {
            final Object loadedData = new Yaml().load(input);
            if (loadedData == null) {
                document = new StructuredConfigurationDocument();
                loaded = true;
                log.warn("Your file '" + filePath + "' seems to be empty. Is this right?");
                return;
            }
            if (!(loadedData instanceof final Map<?, ?> map)) {
                loaded = false;
                log.error("Failed to load configuration from file: " + filePath
                        + ". The YAML root must be a section.");
                return;
            }
            document = new StructuredConfigurationDocument(map);
            loaded = true;
        } catch (final FileNotFoundException e) {
            loaded = false;
            log.error("Failed to load configuration from file: " + filePath, e);
        } catch (final IOException e) {
            loaded = false;
            log.error("IO Exception while loading configuration from file: " + filePath, e);
        } catch (final YAMLException | ClassCastException e) {
            loaded = false;
            log.error("Malformed YAML while loading configuration from file: " + filePath, e);
        }
    }

    @SuppressWarnings("PMD.AvoidFileStream")
    private void saveToFile() {
        final DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        final Yaml yaml = new Yaml(options);

        try (OutputStreamWriter writer = new OutputStreamWriter(new java.io.FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            yaml.dump(document.asMap(), writer);
        } catch (final IOException e) {
            log.error("Failed to save configuration to file: " + filePath, e);
        }
    }

    private void logInvalidFallback(final String key, final String expectedType, final Object actual, final Object def) {
        if (actual != null) {
            log.warn("Invalid config value for '" + key + "'. Expected " + expectedType
                    + " but found " + actual.getClass().getSimpleName() + ". Falling back to default: " + def);
        }
    }
}
