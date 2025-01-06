package com.jannik_kuehn.common.storage;

import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileStorageProviderTest {

    private Configuration mockConfig;

    private FileStorageProvider storageProvider;

    @BeforeEach
    public void setUp() {
        mockConfig = mock(Configuration.class);
        storageProvider = new FileStorageProvider(mockConfig);
    }

    @Test
    void testReadWithString() throws StorageException {
        final String key = "testKey";
        final String value = "testValue";

        when(mockConfig.getObject(key)).thenReturn(value);

        final Object result = storageProvider.read(key);
        assertEquals(value, result, "The read value should be the same as the mocked value. Expected: " + value);
        verify(mockConfig, times(1)).getObject(key);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    void testReadWithSet() throws StorageException {
        when(mockConfig.getObject("key1")).thenReturn("value1");
        when(mockConfig.getObject("key2")).thenReturn("value2");

        final Set<String> keys = Set.of("key1", "key2");
        final Map<String, Object> result = storageProvider.read(keys);

        assertEquals(2, result.size(), "Result size should match the number of keys");
        assertEquals("value1", result.get("key1"), "Value for key1 should match");
        assertEquals("value2", result.get("key2"), "Value for key2 should match");

        verify(mockConfig, times(1)).getObject("key1");
        verify(mockConfig, times(1)).getObject("key2");
    }

    @Test
    void testReadAll() throws StorageException {
        final Map<String, Object> map = Map.of("key1", "value1", "key2", "value2");
        when(mockConfig.getAll()).thenReturn(map);

        final Map<String, Object> result = storageProvider.readAll();
        assertEquals(map, result, "The read map should be the same as the mocked map.");
        verify(mockConfig, times(1)).getAll();
    }

    @Test
    void testWrite() throws StorageException {
        final String key = "testKey";
        final String value = "testValue";

        storageProvider.write(key, value);

        verify(mockConfig, times(1)).setValue(key, value);
    }

    @Test
    void testWriteWithOverwrite() throws StorageException {
        final String key1 = "key1";
        final String key2 = "key2";
        final String value = "sameValue";

        when(mockConfig.getAll()).thenReturn(Map.of(key1, value));

        storageProvider.write(key2, value, true);

        verify(mockConfig, times(1)).remove(key1);
        verify(mockConfig, times(1)).setValue(key2, value);
    }

    @Test
    void testWriteAll() throws StorageException {
        final String key1 = "key1";
        final String key2 = "key2";
        final String value1 = "value1";
        final String value2 = "value2";

        final Map<String, Object> map = Map.of(key1, value1, key2, value2);

        storageProvider.writeAll(map);

        verify(mockConfig, times(1)).setValue(key1, value1);
        verify(mockConfig, times(1)).setValue(key2, value2);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    void testWriteAllWithOverwrite() throws StorageException {
        when(mockConfig.getAll()).thenReturn(Map.of("key1", "value1", "key3", "value2"));

        final Map<String, Object> data = Map.of("key2", "value1", "key4", "value3");

        storageProvider.writeAll(data, true);

        verify(mockConfig, times(1)).remove("key1"); // Wert "value1" wurde überschrieben
        verify(mockConfig, times(1)).setValue("key2", "value1");
        verify(mockConfig, times(1)).setValue("key4", "value3");
    }

    @Test
    void checkClosed() throws StorageException {
        assertDoesNotThrow(storageProvider::readAll);
        storageProvider.close();
        assertThrows(StorageException.class, storageProvider::readAll, "The storage provider should throw an exception when closed.");
    }
}
