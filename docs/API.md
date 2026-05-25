## API

The LoriTime API lets other plugins read stored player online time, resolve known player identities, and write audited manual time adjustments.

## Compatibility

| Platform | LoriTime version | Supported |
|----------|------------------|-----------|
| Paper    | 2.0.0 - current  | ✅       |
| Folia    | 2.0.0 - current  | ✅       |
| Velocity | 2.0.0 - current  | ✅       |
| Bungee   | 2.0.0 - current  | ✅       |

## Dependency Setup

There is currently no public Maven or Gradle repository for LoriTime. Add the LoriTime jar to your build manually and declare a plugin dependency or soft dependency.

### Paper / Folia

```yml
name: MyPlugin
version: 1.0
main: myplugin.MyPlugin
author: MaxMustermann
description: A plugin that hooks into LoriTime.
softdepend:
  - LoriTime
```

Use `depend` instead of `softdepend` if your plugin cannot run without LoriTime.

### Bungee

```yml
name: MyPlugin
version: 1.0
main: myplugin.MyPlugin
author: MaxMustermann
softDepends:
  - LoriTime
```

### Velocity

Add LoriTime as a dependency in your Velocity plugin annotation or plugin metadata according to your build setup.

## Recommended Facade

Use `LoriTimeAPI.service()` for new integrations. It returns an `Optional<LoriTimeService>` because LoriTime may not be installed or initialized yet.

```java
import com.jannik_kuehn.common.api.LoriTimeAPI;
import com.jannik_kuehn.common.api.LoriTimeService;

public final class LoriTimeHook {

    private LoriTimeService loriTime;

    public void enable() {
        LoriTimeAPI.service().ifPresent(service -> this.loriTime = service);
    }
}
```

If LoriTime is a hard dependency and your plugin enables after LoriTime, the optional should normally be present during your enable phase.

## Reading Player Data

## Public Player Model

`LoriTimePlayer` is the stable public player identity contract. It exposes only the player's UUID and latest known name. Time operations always use the UUID as the exact player identity; the name is display or audit metadata.

Use `LoriTimePlayerRef` when your plugin needs to keep or create a player reference:

```java
LoriTimePlayer player = new LoriTimePlayerRef(uniqueId, playerName);
```

LoriTime internals may use richer sender objects for permissions, messages, console state, or online state. Those sender details are not part of the public `LoriTimePlayer` contract.

### Resolve UUID by Name

```java
loriTime.findUuid("Lorias_").thenAccept(optionalUniqueId -> {
    optionalUniqueId.ifPresent(uniqueId -> {
        // Use the UUID in your plugin.
    });
});
```

### Resolve Latest Name by UUID

```java
loriTime.findName(uniqueId).thenAccept(optionalName -> {
    optionalName.ifPresent(name -> {
        // Use the latest known name in your plugin.
    });
});
```

### Read Online Time

```java
loriTime.getOnlineTime(uniqueId).thenAccept(optionalOnlineTime -> {
    optionalOnlineTime.ifPresent(duration -> {
        long seconds = duration.toSeconds();
    });
});
```

If you already have a `LoriTimePlayer`, use the player overload:

```java
loriTime.getOnlineTime(player).thenAccept(optionalOnlineTime -> {
    optionalOnlineTime.ifPresent(duration -> {
        // Use the player's time.
    });
});
```

An empty result means LoriTime does not currently have stored data for that player.

### Read Scoped Online Time

Use `TimeScope` to query global, server, or world totals:

```java
import com.jannik_kuehn.common.api.storage.TimeScope;

loriTime.getOnlineTime(uniqueId, TimeScope.server("survival")).thenAccept(optionalOnlineTime -> {
    optionalOnlineTime.ifPresent(duration -> {
        long seconds = duration.toSeconds();
    });
});

loriTime.getOnlineTime(uniqueId, TimeScope.world("survival", "world"));
```

### Read Time In A Range

Use `TimeRange` to query a bounded inclusive-start/exclusive-end history window. Ranged totals clip session rows to the requested window and include manual adjustments whose audit timestamp is inside the range.

```java
import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;

TimeRange lastSevenDays = TimeRange.between(
        Instant.now().minus(Duration.ofDays(7)),
        Instant.now()
);

loriTime.getOnlineTime(uniqueId, TimeScope.server("survival"), lastSevenDays)
        .thenAccept(optionalOnlineTime -> {
            optionalOnlineTime.ifPresent(duration -> {
                long seconds = duration.toSeconds();
            });
        });
```

The ranged overload is part of the LoriTime 2 API surface. Storage implementations of `TimeQueryStorage` and `UnifiedStorage` must implement the ranged `getTime(UUID, TimeScope, TimeRange)` contract.

## Writing Manual Adjustments

Use signed durations. Positive values add time, negative values remove time. Durations must be precise to whole seconds.

### System/API Adjustment

```java
loriTime.addTime(uniqueId, Duration.ofMinutes(10)).thenRun(() -> {
    // The write was attempted successfully.
});
loriTime.addTime(uniqueId, Duration.ofMinutes(-5));
```

The same operation can target a public player reference:

```java
loriTime.addTime(player, Duration.ofMinutes(10));
```

These adjustments are stored with LoriTime's stable API actor name.

To write a scoped adjustment, pass a `TimeScope`:

```java
loriTime.addTime(uniqueId, Duration.ofMinutes(10), TimeScope.server("survival"));
loriTime.addTime(uniqueId, Duration.ofMinutes(-5), TimeScope.world("survival", "world"));
```

### Actor-Aware Adjustment

```java
loriTime.addTime(
        uniqueId,
        Duration.ofMinutes(10),
        actorUniqueId,
        actorName
).thenRun(() -> {
    // The actor-aware write completed.
});
```

Or pass public player identities for both target and actor:

```java
loriTime.addTime(
        targetPlayer,
        Duration.ofMinutes(10),
        actorPlayer
);
```

Actor-aware adjustments preserve the actor UUID and actor name in LoriTime's adjustment history.

Actor-aware adjustments can also be scoped:

```java
loriTime.addTime(uniqueId, Duration.ofMinutes(10), actorUniqueId, actorName, TimeScope.server("survival"));
```

## Errors

Facade methods validate null inputs and unsupported duration values before scheduling storage work. Storage failures complete the returned future exceptionally with `LoriTimeApiException`, so integrations do not need to handle raw storage or SQL exceptions from the public facade.

```java
loriTime.addTime(uniqueId, Duration.ofSeconds(30)).exceptionally(ex -> {
    Throwable cause = ex.getCause();
    if (cause instanceof LoriTimeApiException apiException) {
        getLogger().warning("Could not update LoriTime: " + apiException.getMessage());
    }
    return null;
});
```

## Threading and Storage Modes

The facade returns `CompletableFuture` values. LoriTime schedules the blocking storage work asynchronously, and future continuations run in that asynchronous completion context unless you reschedule them. If your continuation touches Bukkit, Folia, Velocity, Bungee, or other platform-thread-bound APIs, reschedule that work through your platform scheduler first.

In slave mode, facade reads follow the same deterministic fallback behavior as LoriTime's local consumers. If the local instance only has cached data, an unknown player or cache miss may return an empty result until LoriTime receives data from the master.

## API Surface

`LoriTimeAPI.service()`, `LoriTimeService`, `LoriTimePlayer`, and immutable public player references are the stable public integration surface for normal third-party plugins.

Internal classes such as `LoriTimePlugin`, storage lifecycle managers, accumulators, configuration, localization, and updater state are not part of the public integration API. Storage contracts such as `UnifiedStorage` and `TimeAccumulator` remain internal runtime contracts unless a future change introduces a dedicated advanced extension API.
