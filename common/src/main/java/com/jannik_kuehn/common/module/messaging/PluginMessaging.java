package com.jannik_kuehn.common.module.messaging;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.UuidUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public abstract class PluginMessaging {
    protected static final String AFK_IDENTIFIER = "loritime:afk";

    protected static final String SLAVED_TIME_STORAGE = "loritime:storage";

    protected final LoriTimePlugin loriTimePlugin;

    private final LoriTimeLogger log;

    public PluginMessaging(final LoriTimePlugin loriTimePlugin) {
        this.loriTimePlugin = loriTimePlugin;
        this.log = loriTimePlugin.getLoggerFactory().create(PluginMessaging.class);
    }

    public abstract void sendPluginMessage(String channelIdentifier, Object... message);

    protected byte[] getDataAsByte(final Object... message) {
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
                } else if (part instanceof UUID) {
                    out.write(UuidUtil.toBytes((UUID) part));
                } else {
                    throw new IOException("invalid data " + part.toString());
                }
            }
            return byteOut.toByteArray();
        } catch (final IOException e) {
            log.warn("could not serialize plugin message", e);
        }
        return null;
    }

    protected void processPluginMessage(final String identifier, final byte[] data) {
        loriTimePlugin.getScheduler().runAsyncOnce(() -> {
            if (identifier.equalsIgnoreCase(AFK_IDENTIFIER)) {
                setAfkStatus(data);
            } else if (identifier.equalsIgnoreCase(SLAVED_TIME_STORAGE)) {
                slavedTimeStorageHandling(data);
            }
        });
    }

    private void setAfkStatus(final byte[] data) {
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data);
             DataInputStream input = new DataInputStream(byteInputStream)) {

            final byte[] uuidBytes = new byte[16];
            input.readFully(uuidBytes);
            final UUID playerUUID = UuidUtil.fromBytes(uuidBytes);
            final Optional<CommonSender> optionalPlayer = loriTimePlugin.getServer().getPlayer(playerUUID);
            if (optionalPlayer.isEmpty()) {
                return;
            }
            final LoriTimePlayer player = new LoriTimePlayer(playerUUID, optionalPlayer.get().getName());

            switch (input.readUTF()) {
                case "true":
                    loriTimePlugin.getAfkStatusProvider().setPlayerAFK(player, input.readLong());
                    break;
                case "false":
                    loriTimePlugin.getAfkStatusProvider().resumePlayerAFK(player);
                    break;
                default:
                    log.warn("received invalid afk status!");
            }
        } catch (final IOException e) {
            log.error("could not deserialize plugin message", e);
        }
    }

    private void slavedTimeStorageHandling(final byte[] data) {
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data);
             DataInputStream input = new DataInputStream(byteInputStream)) {

            final byte[] uuidBytes = new byte[16];
            input.readFully(uuidBytes);
            final UUID playerUUID = UuidUtil.fromBytes(uuidBytes);
            final String inputString = input.readUTF();
            switch (inputString) {
                case "get":
                    sendPluginMessage(SLAVED_TIME_STORAGE, playerUUID, "send", getTime(playerUUID));
                    break;
                case "add":
                    loriTimePlugin.getTimeStorage().addTime(playerUUID, input.readLong());
                    break;
                default:
                    log.warn("received invalid status: " + inputString);
            }
        } catch (final IOException e) {
            log.error("could not deserialize plugin message", e);
        } catch (final StorageException e) {
            throw new RuntimeException(e);
        }
    }

    private long getTime(final UUID playerUUID) {
        OptionalLong time = OptionalLong.empty();
        try {
            time = loriTimePlugin.getTimeStorage().getTime(playerUUID);
        } catch (final StorageException e) {
            log.error("could not get time for " + playerUUID, e);
        }
        if (time.isPresent()) {
            return time.getAsLong();
        }
        return 0L;
    }
}
