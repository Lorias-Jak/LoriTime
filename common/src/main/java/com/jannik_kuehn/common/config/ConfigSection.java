package com.jannik_kuehn.common.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a path-scoped view of a structured configuration document.
 */
public interface ConfigSection {
    /**
     * Returns the absolute path represented by this section.
     *
     * @return section path, or an empty string for the root section
     */
    String getPath();

    /**
     * Returns direct child keys of this section.
     *
     * @return direct child keys
     */
    Set<String> getKeys();

    /**
     * Returns child keys of this section.
     *
     * @param recursive if true, nested child paths are included
     * @return child keys relative to this section
     */
    Set<String> getKeys(boolean recursive);

    /**
     * Checks if a section-relative path exists.
     *
     * @param path section-relative path
     * @return true if the path exists
     */
    boolean contains(String path);

    /**
     * Returns a nested section for a section-relative path.
     *
     * @param path section-relative path
     * @return nested section when the path exists and is a section
     */
    Optional<ConfigSection> getSection(String path);

    /**
     * Creates or replaces a nested section at a section-relative path.
     *
     * @param path section-relative path
     * @return created section
     */
    ConfigSection createSection(String path);

    /**
     * Reads an object from a section-relative path.
     *
     * @param path section-relative path
     * @return configured value, or null when missing
     */
    Object getObject(String path);

    /**
     * Reads an object from a section-relative path.
     *
     * @param path section-relative path
     * @param def  fallback value
     * @return configured value, or fallback when missing
     */
    Object getObject(String path, Object def);

    /**
     * Reads a string from a section-relative path.
     *
     * @param path section-relative path
     * @return configured string, or null when missing or incompatible
     */
    String getString(String path);

    /**
     * Reads a string from a section-relative path.
     *
     * @param path section-relative path
     * @param def  fallback value
     * @return configured string, or fallback when missing or incompatible
     */
    String getString(String path, String def);

    /**
     * Reads an integer from a section-relative path.
     *
     * @param path section-relative path
     * @return configured integer, or zero when missing or incompatible
     */
    int getInt(String path);

    /**
     * Reads an integer from a section-relative path.
     *
     * @param path section-relative path
     * @param def  fallback value
     * @return configured integer, or fallback when missing or incompatible
     */
    int getInt(String path, int def);

    /**
     * Reads a long from a section-relative path.
     *
     * @param path section-relative path
     * @return configured long, or zero when missing or incompatible
     */
    long getLong(String path);

    /**
     * Reads a long from a section-relative path.
     *
     * @param path section-relative path
     * @param def  fallback value
     * @return configured long, or fallback when missing or incompatible
     */
    long getLong(String path, long def);

    /**
     * Reads a boolean from a section-relative path.
     *
     * @param path section-relative path
     * @return configured boolean, or false when missing or incompatible
     */
    boolean getBoolean(String path);

    /**
     * Reads a boolean from a section-relative path.
     *
     * @param path section-relative path
     * @param def  fallback value
     * @return configured boolean, or fallback when missing or incompatible
     */
    boolean getBoolean(String path, boolean def);

    /**
     * Reads a list from a section-relative path.
     *
     * @param path section-relative path
     * @return configured list, or an empty list when missing or incompatible
     */
    List<?> getArrayList(String path);

    /**
     * Returns values in this section.
     *
     * @param recursive if true, nested values are returned as relative dot paths
     * @return values relative to this section
     */
    Map<String, Object> getValues(boolean recursive);
}
