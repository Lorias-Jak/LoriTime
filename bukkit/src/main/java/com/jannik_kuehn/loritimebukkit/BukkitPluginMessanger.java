package com.jannik_kuehn.loritimebukkit;

import com.jannik_kuehn.common.LoriTimePlugin;
import org.bukkit.plugin.messaging.PluginMessageRecipient;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BukkitPluginMessanger {

    private final LoriTimeBukkit bukkitPlugin;

    private final LoriTimePlugin loriTimePlugin;

    public BukkitPluginMessanger(final LoriTimeBukkit bukkitPlugin) {
        this.bukkitPlugin = bukkitPlugin;
        this.loriTimePlugin = bukkitPlugin.getPlugin();
    }

    public void sendPluginMessage(final PluginMessageRecipient target, final String channel, final Object... message) {
        final byte[] data = getDataAsByte(message);
        if (data != null) {
            target.sendPluginMessage(bukkitPlugin, channel, data);
        } else {
            loriTimePlugin.getLogger().warning("could not send plugin message, data is null");
        }
    }

    private byte[] getDataAsByte(final Object... message) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteOut)) {
            for (final Object part : message) {
                if (part instanceof String) {
                    out.writeUTF((String) part);
                } else if (part instanceof Integer) {
                    out.writeInt((Integer) part);
                } else if (part instanceof Long) {
                    out.writeLong((Long) part);
                } else if (part instanceof Float) {
                    out.writeFloat((Float) part);
                } else if (part instanceof Double) {
                    out.writeDouble((Double) part);
                } else if (part instanceof Boolean) {
                    out.writeBoolean((Boolean) part);
                } else {
                    throw new IOException("invalid data " + part.toString());
                }
            }
            return byteOut.toByteArray();
        } catch (final IOException e) {
            loriTimePlugin.getLogger().warning("could not serialize plugin message", e);
        }
        return null;
    }
}
