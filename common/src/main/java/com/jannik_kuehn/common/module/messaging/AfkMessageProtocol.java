package com.jannik_kuehn.common.module.messaging;

import com.jannik_kuehn.common.module.afk.AfkTransitionType;

import java.util.Arrays;
import java.util.Optional;

/**
 * Versioned AFK plugin message protocol.
 */
public final class AfkMessageProtocol {

    /**
     * Current AFK plugin message protocol version.
     */
    public static final int VERSION = 2;

    private AfkMessageProtocol() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Parses a transition payload value.
     *
     * @param value raw payload value
     * @return transition type when valid
     */
    public static Optional<AfkTransitionType> parseTransition(final String value) {
        return Arrays.stream(AfkTransitionType.values())
                .filter(type -> type.name().equals(value))
                .findFirst();
    }

    /**
     * Checks whether the payload version is supported.
     *
     * @param version raw protocol version
     * @return true when the version is supported
     */
    public static boolean isSupportedVersion(final int version) {
        return version == VERSION;
    }

    /**
     * Checks whether a transition may be sent by a slave.
     *
     * @param transition transition type
     * @return true when slave messages may request the transition
     */
    public static boolean isSlaveTransition(final AfkTransitionType transition) {
        return transition == AfkTransitionType.START || transition == AfkTransitionType.RESUME;
    }
}
