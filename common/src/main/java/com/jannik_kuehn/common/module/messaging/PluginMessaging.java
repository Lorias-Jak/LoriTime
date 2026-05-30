package com.jannik_kuehn.common.module.messaging;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonPlayerSender;
import com.jannik_kuehn.common.exception.PluginMessageException;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.module.afk.AfkTransitionType;
import com.jannik_kuehn.common.player.TrackedLoriTimePlayer;
import com.jannik_kuehn.common.utils.UuidUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * The PluginMessaging class is responsible for sending and computing PluginMessages.
 * They're used to communicate between proxy and subserver.
 */
@SuppressWarnings("PMD.TooManyMethods")
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
     * Current storage plugin message protocol version.
     */
    protected static final int STORAGE_PROTOCOL_VERSION = StorageMessageProtocol.VERSION;

    /**
     * Prefix for ignored storage plugin message warnings.
     */
    private static final String STORAGE_IGNORED_PREFIX = "Storage plugin message ignored for player ";

    /**
     * The {@link LoriTimePlugin} instance.
     */
    protected final LoriTimePlugin loriTimePlugin;

    /**
     * The {@link WrappedLogger} instance.
     */
    private final WrappedLogger log;

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
                    log.error("could not serialize plugin message. Invalid data: " + part.toString());
                }
            }
            return byteOut.toByteArray();
        } catch (final IOException e) {
            log.error("could not serialize plugin message", new PluginMessageException(e));
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
        log.debug("Processing AFK plugin message");
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data);
             DataInputStream input = new DataInputStream(byteInputStream)) {

            final byte[] uuidBytes = new byte[16];
            input.readFully(uuidBytes);
            final UUID playerUUID = UuidUtil.fromBytes(uuidBytes);
            final Optional<CommonPlayerSender> optionalPlayer = loriTimePlugin.getServer().getPlayer(playerUUID);
            if (optionalPlayer.isEmpty()) {
                log.debug("Player with the uuid '" + playerUUID + "' is not online or cant be found");
                return;
            }
            final TrackedLoriTimePlayer player = loriTimePlugin.getPlayerConverter().getOnlinePlayer(playerUUID);

            final int protocolVersion = input.readInt();
            if (!AfkMessageProtocol.isSupportedVersion(protocolVersion)) {
                log.warn("AFK plugin message ignored: unsupported protocol version " + protocolVersion
                        + " for player " + playerUUID);
                return;
            }
            final String transitionValue = input.readUTF();
            final Optional<AfkTransitionType> transition = AfkMessageProtocol.parseTransition(transitionValue);
            if (transition.isEmpty()) {
                log.warn("AFK plugin message ignored for player " + playerUUID
                        + ": invalid transition '" + transitionValue + "'");
                return;
            }
            if (!AfkMessageProtocol.isSlaveTransition(transition.get())) {
                log.warn("AFK plugin message ignored for player " + playerUUID
                        + ": transition '" + transitionValue + "' is not accepted from slaves");
                return;
            }
            if (transition.get() == AfkTransitionType.START) {
                log.debug("Setting player '" + player.getName() + "' to AFK");
                loriTimePlugin.getAfkStatusProvider().setPlayerAFK(player, input.readLong());
            } else {
                log.debug("Resuming player '" + player.getName() + "' from AFK");
                loriTimePlugin.getAfkStatusProvider().resumePlayerAFK(player);
            }
        } catch (final EOFException e) {
            log.warn("AFK plugin message ignored: malformed payload");
        } catch (final IOException e) {
            final PluginMessageException pluginMessageException = new PluginMessageException(e);
            log.error("AFK plugin message failed during decoding", pluginMessageException);
        }
    }

    private void slavedTimeStorageHandling(final byte[] data) {
        log.debug("Processing storage plugin message");
        String operationValue = "<unknown>";
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data);
             DataInputStream input = new DataInputStream(byteInputStream)) {

            final byte[] uuidBytes = new byte[16];
            input.readFully(uuidBytes);
            final UUID playerUUID = UuidUtil.fromBytes(uuidBytes);
            operationValue = input.readUTF();
            final Optional<StorageMessageType> messageType = StorageMessageProtocol.parseType(operationValue);
            if (messageType.isEmpty()) {
                warnStorageIgnored(playerUUID, "unknown operation '" + operationValue + "'");
                return;
            }
            handleStorageMessage(playerUUID, messageType.get(), input);
        } catch (final EOFException e) {
            log.warn("Storage plugin message ignored: malformed payload for operation '" + operationValue + "'");
        } catch (final IOException e) {
            final PluginMessageException pluginMessageException = new PluginMessageException(e);
            log.error("Storage plugin message failed during decoding for operation '" + operationValue + "'",
                    pluginMessageException);
        } catch (final StorageException e) {
            log.error("Storage plugin message failed while applying operation '" + operationValue + "'", e);
        }
    }

    private void handleStorageMessage(final UUID playerUUID, final StorageMessageType messageType, final DataInputStream input)
            throws IOException, StorageException {
        switch (messageType) {
            case GET:
                log.debug("Sending time for player '" + playerUUID + "'");
                sendPluginMessage(SLAVED_TIME_STORAGE, playerUUID, StorageMessageType.SEND.wireValue(), getTime(playerUUID));
                break;
            case ADD:
                log.debug("Adding time for player '" + playerUUID + "'");
                loriTimePlugin.getStorage().addTime(playerUUID, input.readLong());
                break;
            case SESSION:
                rejectRemoteSession(playerUUID, input);
                break;
            case WORLD:
                updateRemoteWorldContext(playerUUID, input);
                break;
            case WORLD_SWITCH:
                switchRemoteWorldContext(playerUUID, input);
                break;
            case SEND:
                warnStorageIgnored(playerUUID, "operation '" + messageType.wireValue() + "' is not accepted by the master");
                break;
        }
    }

    private void rejectRemoteSession(final UUID playerUUID, final DataInputStream input) throws IOException {
        final int protocolVersion = input.readInt();
        if (!isSupportedStorageVersion(StorageMessageType.SESSION, protocolVersion, playerUUID)) {
            return;
        }
        warnStorageIgnored(playerUUID, "stale completed remote session payload");
    }

    private void updateRemoteWorldContext(final UUID playerUUID, final DataInputStream input) throws IOException, StorageException {
        if (!isSupportedStorageVersion(StorageMessageType.WORLD, input.readInt(), playerUUID)) {
            return;
        }
        final String world = input.readUTF();
        final long observedAtMs = input.readLong();
        loriTimePlugin.getAccumulator().updateWorldContext(playerUUID, world, observedAtMs);
        loriTimePlugin.getAccumulator().getActiveSessionContext(playerUUID)
                .ifPresent(context -> loriTimePlugin.rememberScope(context.server(), world));
    }

    private void switchRemoteWorldContext(final UUID playerUUID, final DataInputStream input) throws IOException, StorageException {
        if (!isSupportedStorageVersion(StorageMessageType.WORLD_SWITCH, input.readInt(), playerUUID)) {
            return;
        }
        final String world = input.readUTF();
        final long observedAtMs = input.readLong();
        loriTimePlugin.getAccumulator().switchWorldContext(playerUUID, world, observedAtMs);
        loriTimePlugin.getAccumulator().getActiveSessionContext(playerUUID)
                .ifPresent(context -> loriTimePlugin.rememberScope(context.server(), world));
    }

    private boolean isSupportedStorageVersion(final StorageMessageType messageType, final int protocolVersion, final UUID playerUUID) {
        if (protocolVersion == StorageMessageProtocol.VERSION) {
            return true;
        }
        warnStorageIgnored(playerUUID, "operation '" + messageType.wireValue()
                + "' uses unsupported protocol version " + protocolVersion);
        return false;
    }

    private void warnStorageIgnored(final UUID playerUUID, final String reason) {
        log.warn(STORAGE_IGNORED_PREFIX + playerUUID + ": " + reason);
    }

    private long getTime(final UUID playerUUID) {
        OptionalLong time = OptionalLong.empty();
        try {
            time = loriTimePlugin.getStorage().getTime(playerUUID);
        } catch (final StorageException e) {
            log.error("could not get time for " + playerUUID, e);
        }
        if (time.isPresent()) {
            return time.getAsLong();
        }
        return 0L;
    }
}
