package com.jannik_kuehn.loritime.bukkit;

import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import org.bukkit.plugin.messaging.PluginMessageRecipient;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BukkitPluginMessanger {

    private final LoriTimeBukkit bukkitPlugin;
    private final LoriTimePlugin loriTimePlugin;

    public BukkitPluginMessanger(LoriTimeBukkit bukkitPlugin) {
        this.bukkitPlugin = bukkitPlugin;
        this.loriTimePlugin = bukkitPlugin.getLoriTimePlugin();
    }

    public void sendPluginMessage(PluginMessageRecipient target, String channel, Object... message) {
        byte[] data = getDataAsByte(message);
        if (data != null) {
            target.sendPluginMessage(bukkitPlugin, channel, data);
        } else {
            loriTimePlugin.getLogger().warning("could not send plugin message, data is null");
        }
    }

    private byte[] getDataAsByte(Object... message) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteOut)) {
            for (Object part : message) {
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
        } catch (IOException e) {
            loriTimePlugin.getLogger().warning("could not serialize plugin message", e);
        }
        return null;
    }
}
