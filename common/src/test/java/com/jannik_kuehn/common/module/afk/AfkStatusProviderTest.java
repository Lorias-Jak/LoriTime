package com.jannik_kuehn.common.module.afk;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.platform.CommonServer;
import com.jannik_kuehn.common.scheduler.PluginScheduler;
import com.jannik_kuehn.common.scheduler.PluginTask;
import com.jannik_kuehn.common.utils.TimeParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AfkStatusProviderTest {

    @Test
    void proxyDoesNotStartRepeatedAfkCheck() {
        final PluginScheduler scheduler = mock(PluginScheduler.class);
        final TestContext context = new TestContext(true, true, scheduler);

        new AfkStatusProvider(context.plugin(), new NoopAfkHandling(context.plugin()));

        verify(scheduler, never()).scheduleAsync(anyLong(), anyLong(), any());
    }

    @Test
    void restartAfkCheckCancelsExistingTaskBeforeSchedulingReplacement() {
        final CapturingScheduler scheduler = new CapturingScheduler();
        final TestContext context = new TestContext(false, true, scheduler);
        final AfkStatusProvider provider = new AfkStatusProvider(context.plugin(), new NoopAfkHandling(context.plugin()));

        provider.restartAfkCheck();

        assertEquals(List.of(true, false), scheduler.cancelledStates(),
                "Expected the previous task to be cancelled before scheduling the replacement");
    }

    @Test
    void restartAfkCheckStopsSchedulingWhenAfkIsDisabled() {
        final CapturingScheduler scheduler = new CapturingScheduler();
        final TestContext context = new TestContext(false, true, scheduler);
        final AfkStatusProvider provider = new AfkStatusProvider(context.plugin(), new NoopAfkHandling(context.plugin()));

        context.afkEnabled().set(false);
        provider.reloadConfigValues();
        provider.restartAfkCheck();

        assertEquals(List.of(true), scheduler.cancelledStates(), "Expected no replacement task when AFK is disabled");
    }

    @Test
    void restartAfkCheckUsesUpdatedRepeatCheckValue() {
        final CapturingScheduler scheduler = new CapturingScheduler();
        final TestContext context = new TestContext(false, true, scheduler);
        final AfkStatusProvider provider = new AfkStatusProvider(context.plugin(), new NoopAfkHandling(context.plugin()));

        context.repeatCheck().set(60);
        provider.reloadConfigValues();
        provider.restartAfkCheck();

        assertEquals(new Schedule(30L, 60L), scheduler.schedules.getLast(),
                "Expected scheduling to use the updated repeatCheck");
    }

    private record TestContext(LoriTimePlugin plugin, AtomicBoolean afkEnabled, AtomicInteger repeatCheck) {

        private TestContext(final boolean proxy, final boolean afkEnabled, final PluginScheduler scheduler) {
            this(mock(LoriTimePlugin.class), new AtomicBoolean(afkEnabled), new AtomicInteger(30));
            final CommonServer server = mock(CommonServer.class);
            final Configuration config = mock(Configuration.class);
            when(plugin().getLoggerFactory()).thenReturn(new LoggerFactory(Logger.getLogger("test")));
            when(plugin().getConfig()).thenReturn(config);
            when(plugin().getParser()).thenReturn(new TimeParser.Builder().addUnit(60, "m").build());
            when(plugin().getScheduler()).thenReturn(scheduler);
            when(plugin().getServer()).thenReturn(server);
            when(server.isProxy()).thenReturn(proxy);
            when(config.getBoolean(eq("afk.enabled"), eq(false))).thenAnswer(invocation -> this.afkEnabled.get());
            when(config.getBoolean(eq("afk.removeTime"), eq(true))).thenReturn(true);
            when(config.getBoolean(eq("afk.autoKick"), eq(true))).thenReturn(true);
            when(config.getString(eq("afk.after"), eq("15m"))).thenReturn("15m");
            when(config.getInt(eq("afk.repeatCheck"), eq(30))).thenAnswer(invocation -> repeatCheck().get());
        }
    }

    private static final class CapturingScheduler implements PluginScheduler {

        private final List<CapturingTask> tasks = new ArrayList<>();

        private final List<Schedule> schedules = new ArrayList<>();

        @Override
        public PluginTask runAsyncOnce(final Runnable task) {
            task.run();
            return new CapturingTask();
        }

        @Override
        public PluginTask runAsyncOnceLater(final long delay, final Runnable task) {
            return new CapturingTask();
        }

        @Override
        public PluginTask scheduleAsync(final long delay, final long interval, final Runnable task) {
            final CapturingTask scheduledTask = new CapturingTask();
            tasks.add(scheduledTask);
            schedules.add(new Schedule(delay, interval));
            return scheduledTask;
        }

        @Override
        public PluginTask scheduleSync(final Runnable task) {
            task.run();
            return new CapturingTask();
        }

        private List<Boolean> cancelledStates() {
            return tasks.stream()
                    .map(CapturingTask::isCancelled)
                    .toList();
        }
    }

    private static final class CapturingTask implements PluginTask {

        private boolean cancelledFlag;

        @Override
        public void cancel() {
            cancelledFlag = true;
        }

        private boolean isCancelled() {
            return cancelledFlag;
        }
    }

    private record Schedule(long delay, long interval) {
    }

    private static final class NoopAfkHandling extends AfkHandling {

        private NoopAfkHandling(final LoriTimePlugin plugin) {
            super(plugin);
        }

        @Override
        public void executePlayerAfk(final com.jannik_kuehn.common.api.LoriTimePlayer loriTimePlayer,
                                     final long timeToRemove) {
            // Test double intentionally has no AFK side effects.
        }

        @Override
        public void executePlayerResume(final com.jannik_kuehn.common.api.LoriTimePlayer loriTimePlayer) {
            // Test double intentionally has no resume side effects.
        }
    }
}
