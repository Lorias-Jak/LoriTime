package com.jannik_kuehn.loritimebungee.util;

import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.ProxyServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class BungeeServerTest {

    @Test
    void reportsProxyCapability() {
        final BungeeServer server = new BungeeServer(mock(ProxyServer.class), "test", mock(BungeeAudiences.class));

        assertTrue(server.isProxy(), "Expected Bungee to report proxy capability");
    }
}
