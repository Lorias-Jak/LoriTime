package com.jannik_kuehn.common.command;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.command.config.CommandAlias;
import com.jannik_kuehn.common.command.config.CommandAliasConfig;
import com.jannik_kuehn.common.command.core.CommandDefinition;
import com.jannik_kuehn.common.command.core.CommandDispatcher;
import com.jannik_kuehn.common.command.core.CommandExecutionPolicy;
import com.jannik_kuehn.common.command.profile.CommandProfileRegistry;
import com.jannik_kuehn.common.command.profile.RuntimeCommandProfile;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.platform.CommonCommand;
import com.jannik_kuehn.common.platform.CommonSender;
import com.jannik_kuehn.common.scheduler.PluginScheduler;
import com.jannik_kuehn.common.scheduler.PluginTask;
import com.jannik_kuehn.common.utils.TimeParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommandArchitectureTest {

    @Test
    void dispatcherDeniesMissingPermission() {
        final LoriTimePlugin plugin = mock(LoriTimePlugin.class);
        final Localization localization = localization();
        final CommonSender sender = mock(CommonSender.class);
        final AtomicBoolean invoked = new AtomicBoolean();
        final CommandDispatcher dispatcher = new CommandDispatcher(plugin, new CommandDefinition("test", List.of(),
                "loritime.test", CommandExecutionPolicy.SYNC, context -> invoked.set(true), context -> List.of()), localization);

        dispatcher.execute(sender);

        assertFalse(invoked.get(), "Expected handler not to run");
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void dispatcherRunsAsyncHandlersThroughScheduler() {
        final LoriTimePlugin plugin = mock(LoriTimePlugin.class);
        final PluginScheduler scheduler = mock(PluginScheduler.class);
        final CommonSender sender = mock(CommonSender.class);
        final AtomicBoolean invoked = new AtomicBoolean();
        when(plugin.getScheduler()).thenReturn(scheduler);
        when(sender.hasPermission("loritime.test")).thenReturn(true);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return mock(PluginTask.class);
        }).when(scheduler).runAsyncOnce(any());

        final CommandDispatcher dispatcher = new CommandDispatcher(plugin, new CommandDefinition("test", List.of(),
                "loritime.test", CommandExecutionPolicy.ASYNC, context -> invoked.set(true), context -> List.of()),
                localization());
        dispatcher.execute(sender);

        assertTrue(invoked.get(), "Expected async handler to run");
        verify(scheduler).runAsyncOnce(any());
    }

    @Test
    void backendSlaveProfileDoesNotExposeCanonicalCommands() {
        final List<String> commandNames = new CommandProfileRegistry(plugin(false))
                .commands(RuntimeCommandProfile.BACKEND_SLAVE)
                .stream()
                .map(CommonCommand::getCommandName)
                .toList();

        assertEquals(List.of("admin"), commandNames, "Expected backend slave admin commands only");
    }

    @Test
    void backendCanonicalProfileExposesCanonicalAndLocalCommands() {
        final List<String> commandNames = new CommandProfileRegistry(plugin(false))
                .commands(RuntimeCommandProfile.BACKEND_CANONICAL)
                .stream()
                .map(CommonCommand::getCommandName)
                .toList();

        assertEquals(List.of("admin", "time", "top", "stats", "modify"), commandNames,
                "Expected backend canonical commands");
    }

    @Test
    void proxyProfileUsesProxyAliases() {
        final List<String> commandNames = new CommandProfileRegistry(plugin(false))
                .commands(RuntimeCommandProfile.PROXY)
                .stream()
                .map(CommonCommand::getCommandName)
                .toList();

        assertEquals(List.of("admin", "time", "top", "stats", "modify"), commandNames, "Expected proxy commands");
    }

    @Test
    void afkCommandIsRegisteredForBackendWhenEnabled() {
        final List<String> commandNames = new CommandProfileRegistry(plugin(true))
                .commands(RuntimeCommandProfile.BACKEND_SLAVE)
                .stream()
                .map(CommonCommand::getCommandName)
                .toList();

        assertEquals(List.of("admin", "afk"), commandNames,
                "Expected backend slave AFK command when enabled");
    }

    private LoriTimePlugin plugin(final boolean afkEnabled) {
        final LoriTimePlugin plugin = mock(LoriTimePlugin.class);
        final PluginScheduler scheduler = mock(PluginScheduler.class);
        final Configuration config = mock(Configuration.class);
        final CommandAliasConfig aliasConfig = mock(CommandAliasConfig.class);
        final Localization localization = localization();
        when(plugin.getLoggerFactory()).thenReturn(new LoggerFactory(Logger.getLogger("test")));
        when(plugin.getLocalization()).thenReturn(localization);
        when(plugin.getParser()).thenReturn(mock(TimeParser.class));
        when(plugin.getScheduler()).thenReturn(scheduler);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getCommandAliasConfig()).thenReturn(aliasConfig);
        when(plugin.isAfkEnabled()).thenReturn(afkEnabled);
        when(config.getBoolean(anyString())).thenReturn(false);
        when(config.getInt("general.debugAutoDisableTime", 30)).thenReturn(-1);
        doAnswer(invocation -> mock(PluginTask.class)).when(scheduler).runAsyncOnce(any());
        doAnswer(invocation -> {
            final CommandAliasConfig.CommandNode node = invocation.getArgument(1);
            return new CommandAlias(node.configKey(), List.of());
        }).when(aliasConfig).resolve(any(), any(), anyString(), any());
        return plugin;
    }

    private Localization localization() {
        final Localization localization = mock(Localization.class);
        when(localization.getRawMessage(anyString())).thenReturn("message");
        when(localization.formatTextComponent(anyString())).thenReturn(Component.text("message"));
        return localization;
    }
}
