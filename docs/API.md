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

### Resolve UUID by Name

```java
Optional<UUID> uniqueId = loriTime.findUuid("Lorias_");
```

### Resolve Latest Name by UUID

```java
Optional<String> name = loriTime.findName(uniqueId);
```

### Read Online Time

```java
Optional<Duration> onlineTime = loriTime.getOnlineTime(uniqueId);

onlineTime.ifPresent(duration -> {
    long seconds = duration.toSeconds();
});
```

An empty result means LoriTime does not currently have stored data for that player.

## Writing Manual Adjustments

Use signed durations. Positive values add time, negative values remove time. Durations must be precise to whole seconds.

### System/API Adjustment

```java
loriTime.addTime(uniqueId, Duration.ofMinutes(10));
loriTime.addTime(uniqueId, Duration.ofMinutes(-5));
```

These adjustments are stored with LoriTime's stable API actor name.

### Actor-Aware Adjustment

```java
loriTime.addTime(
        uniqueId,
        Duration.ofMinutes(10),
        actorUniqueId,
        actorName
);
```

Actor-aware adjustments preserve the actor UUID and actor name in LoriTime's adjustment history.

## Errors

Facade methods validate null inputs and unsupported duration values before writing. Storage failures are wrapped in `LoriTimeApiException`, so integrations do not need to handle raw storage or SQL exceptions from the public facade.

```java
try {
    loriTime.addTime(uniqueId, Duration.ofSeconds(30));
} catch (LoriTimeApiException ex) {
    getLogger().warning("Could not update LoriTime: " + ex.getMessage());
}
```

## Threading and Storage Modes

The facade is synchronous. Reads and writes can touch database-backed storage or delegated storage/cache behavior depending on LoriTime's configured mode. Avoid running bulk calls on the main server thread; use your platform's async scheduler for repeated, slow, or many-player operations.

In slave mode, facade reads follow the same deterministic fallback behavior as LoriTime's local consumers. If the local instance only has cached data, an unknown player or cache miss may return an empty result until LoriTime receives data from the master.

## API Surface

`LoriTimeAPI.service()` and `LoriTimeService` are the stable public integration surface for normal third-party plugins.

Internal classes such as `LoriTimePlugin`, storage lifecycle managers, accumulators, configuration, localization, and updater state are not part of the public integration API. Storage contracts such as `UnifiedStorage` and `TimeAccumulator` remain internal runtime contracts unless a future change introduces a dedicated advanced extension API.
