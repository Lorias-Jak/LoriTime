package com.jannik_kuehn.common.module.messaging;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.exception.PluginMessageException;
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

/**
 * The PluginMessaging class is responsible for sending and computing PluginMessages.
 * Theyre used to communicate between proxy and subserver.
 */
public abstract class PluginMessaging {
    /**
     * The identifier for the afk-identifier channel.
     */
    protected static final String AFK_IDENTIFIER = "loritime:afk";

    /**
     * The identifier for the storage-identifier channel.
     */
    protected static final String SLAVED_TIME_STORAGE = "loritime:storage";

    /**
     * The {@link LoriTimePlugin} instance.
     */
    protected final LoriTimePlugin loriTimePlugin;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * Creates a new PluginMessaging instance.
     *
     * @param loriTimePlugin the {@link LoriTimePlugin} instance
     */
    public PluginMessaging(final LoriTimePlugin loriTimePlugin) {
        this.loriTimePlugin = loriTimePlugin;
        this.log = loriTimePlugin.getLoggerFactory().create(PluginMessaging.class, "PluginMessaging");
    }

    /**
     * Sends a PluginMessage to the given channel.
     *
     * @param channelIdentifier the identifier of the channel
     * @param message           the objects to send
     */
    public abstract void sendPluginMessage(String channelIdentifier, Object... message);

    /**
     * Converts the given objects to a byte array.
     *
     * @param message the objects to convert
     * @return the byte array
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    protected byte[] getDataAsByte(final Object... message) {
        log.debug("Converting Data for PluginMessage");
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
            final PluginMessageException pluginMessageException = new PluginMessageException(e);
            log.warn("could not serialize plugin message", pluginMessageException);
        }
        return new byte[0];
    }

    /**
     * Processes the received PluginMessage.
     * The identifier is used to determine the type of the message.
     * The data is used to extract the information from the message.
     *
     * @param identifier the identifier of the message
     * @param data       the data of the message
     */
    protected void processPluginMessage(final String identifier, final byte[] data) {
        log.debug("Processing PluginMessage with identifier: " + identifier);
        loriTimePlugin.getScheduler().runAsyncOnce(() -> {
            if (AFK_IDENTIFIER.equalsIgnoreCase(identifier)) {
                setAfkStatus(data);
            } else if (SLAVED_TIME_STORAGE.equalsIgnoreCase(identifier)) {
                slavedTimeStorageHandling(data);
            }
        });
    }

    private void setAfkStatus(final byte[] data) {
        log.debug("Setting AFK Status");
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data);
             DataInputStream input = new DataInputStream(byteInputStream)) {

            final byte[] uuidBytes = new byte[16];
            input.readFully(uuidBytes);
            final UUID playerUUID = UuidUtil.fromBytes(uuidBytes);
            final Optional<CommonSender> optionalPlayer = loriTimePlugin.getServer().getPlayer(playerUUID);
            if (optionalPlayer.isEmpty()) {
                log.debug("Player with the uuid '" + playerUUID + "' is not online or cant be found");
                return;
            }
            final LoriTimePlayer player = loriTimePlugin.getPlayerConverter().getOnlinePlayer(playerUUID);

            switch (input.readUTF()) {
                case "true":
                    log.debug("Setting player '" + player.getName() + "' to AFK");
                    loriTimePlugin.getAfkStatusProvider().setPlayerAFK(player, input.readLong());
                    break;
                case "false":
                    log.debug("Resuming player '" + player.getName() + "' from AFK");
                    loriTimePlugin.getAfkStatusProvider().resumePlayerAFK(player);
                    break;
                default:
                    log.warn("received invalid afk status!");
                    break;
            }
        } catch (final IOException e) {
            final PluginMessageException pluginMessageException = new PluginMessageException(e);
            log.error("could not deserialize plugin message", pluginMessageException);
        }
    }

    private void slavedTimeStorageHandling(final byte[] data) {
        log.debug("Handling Slaved Time Storage");
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data);
             DataInputStream input = new DataInputStream(byteInputStream)) {

            final byte[] uuidBytes = new byte[16];
            input.readFully(uuidBytes);
            final UUID playerUUID = UuidUtil.fromBytes(uuidBytes);
            final String inputString = input.readUTF();
            switch (inputString) {
                case "get":
                    log.debug("Sending time for player '" + playerUUID + "'");
                    sendPluginMessage(SLAVED_TIME_STORAGE, playerUUID, "send", getTime(playerUUID));
                    break;
                case "add":
                    log.debug("Adding time for player '" + playerUUID + "'");
                    loriTimePlugin.getTimeStorage().addTime(playerUUID, input.readLong());
                    break;
                default:
                    log.warn("received invalid status: " + inputString);
                    break;
            }
        } catch (final IOException e) {
            final PluginMessageException pluginMessageException = new PluginMessageException(e);
            log.error("could not deserialize plugin message", pluginMessageException);
        } catch (final StorageException e) {
            log.error("could not add time", e);
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
