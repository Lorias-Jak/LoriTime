package com.jannik_kuehn.common.config.migration;

import com.jannik_kuehn.common.config.StructuredConfigurationDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Ordered set of operations that upgrades a configuration document between schema versions.
 */
public final class ConfigMigration {
    /**
     * Source schema version.
     */
    private final int fromVersion;

    /**
     * Target schema version.
     */
    private final int toVersion;

    /**
     * Operations applied by this migration.
     */
    private final List<Operation> operations;

    private ConfigMigration(final int fromVersion, final int toVersion, final List<Operation> operations) {
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.operations = List.copyOf(operations);
    }

    /**
     * Returns the source schema version.
     *
     * @return source version
     */
    public int getFromVersion() {
        return fromVersion;
    }

    /**
     * Returns the target schema version.
     *
     * @return target version
     */
    public int getToVersion() {
        return toVersion;
    }

    /**
     * Applies this migration to a document.
     *
     * @param document document to mutate
     */
    public void apply(final StructuredConfigurationDocument document) {
        for (final Operation operation : operations) {
            operation.apply(document);
        }
    }

    /**
     * Starts building a migration from the given version to the next version.
     *
     * @param fromVersion source version
     * @return migration builder
     */
    public static Builder from(final int fromVersion) {
        return new Builder(fromVersion, fromVersion + 1);
    }

    /**
     * A mutation applied to a structured configuration document.
     */
    @FunctionalInterface
    public interface Operation {
        /**
         * Applies the operation.
         *
         * @param document document to mutate
         */
        void apply(StructuredConfigurationDocument document);
    }

    /**
     * Builder for config migrations.
     */
    public static final class Builder {
        /**
         * Source schema version.
         */
        private final int fromVersion;

        /**
         * Target schema version.
         */
        private final int toVersion;

        /**
         * Operations collected by the builder.
         */
        private final List<Operation> operations;

        private Builder(final int fromVersion, final int toVersion) {
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
            this.operations = new ArrayList<>();
        }

        /**
         * Adds a missing value.
         *
         * @param path         target path
         * @param defaultValue default value
         * @return this builder
         */
        public Builder add(final String path, final Object defaultValue) {
            operations.add(document -> {
                if (!document.contains(path)) {
                    document.set(path, defaultValue);
                }
            });
            return this;
        }

        /**
         * Renames a value path.
         *
         * @param source source path
         * @param target target path
         * @return this builder
         */
        public Builder rename(final String source, final String target) {
            operations.add(document -> document.move(source, target));
            return this;
        }

        /**
         * Moves a value path.
         *
         * @param source source path
         * @param target target path
         * @return this builder
         */
        public Builder move(final String source, final String target) {
            return rename(source, target);
        }

        /**
         * Deletes a path.
         *
         * @param path path to delete
         * @return this builder
         */
        public Builder delete(final String path) {
            operations.add(document -> document.remove(path));
            return this;
        }

        /**
         * Transforms a value in place.
         *
         * @param path        value path
         * @param transformer value transformer
         * @return this builder
         */
        public Builder transform(final String path, final Function<Object, Object> transformer) {
            operations.add(document -> {
                final Object value = document.get(path);
                if (value != null) {
                    document.set(path, transformer.apply(value));
                }
            });
            return this;
        }

        /**
         * Validates a value and replaces it with a fallback when invalid.
         *
         * @param path      value path
         * @param validator value validator
         * @param fallback  fallback value
         * @return this builder
         */
        public Builder validate(final String path, final Predicate<Object> validator, final Object fallback) {
            operations.add(document -> {
                final Object value = document.get(path);
                if (value != null && !validator.test(value)) {
                    document.set(path, fallback);
                }
            });
            return this;
        }

        /**
         * Adds a custom document operation.
         *
         * @param operation operation to apply
         * @return this builder
         */
        public Builder operation(final Operation operation) {
            operations.add(operation);
            return this;
        }

        /**
         * Builds the migration.
         *
         * @return migration
         */
        public ConfigMigration build() {
            return new ConfigMigration(fromVersion, toVersion, operations);
        }
    }
}
