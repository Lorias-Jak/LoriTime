## Configuration
> ⚠️If you want to use the AFK feature on your proxy, you need to do a [multiSetup](https://github.com/Lorias-Jak/LoriTime/wiki/Setup#setup-instructions---multi-server)! Install the LoriTime jar on your subserver and proxy.<br>
> ⚠️If you use the MultiSetup, enable the AFK feature on the proxy master and on every Paper/Folia slave where players should be detected as AFK. The subserver detects inactivity and sends the AFK state to the proxy; the proxy applies time removal, kicks, and announcements.<br>

* Set `afk.enabled` to `true`
* Set `afk.after` to the wishing time of you, after wich time the player should be kicked. You should use the [TimeString](https://github.com/Lorias-Jak/LoriTime/wiki/Commands-&-Permissions#timestring-examples).
* RemoveTime is for removing the time after which the player is considered afk. So if the player is AFK for 15 minutes, the time will be taken from his online time.
* If `afk.autoKick` is on `true`, the player will automatically get kicked when he is considered afk.
* `afk.repeatedCheck` is the time, after wich the server will check the time the player hasn't moved, interacted or written a message in the chat. Default its 30 seconds. It's recommended
* to not put it under 10 seconds.

## The config part
<details>
<summary>AFK-Config (config.yml)</summary>

```yml
###########
#   AFK   #
###########
afk:

  # In case you're using MultiSetup, enable this on the proxy master and on
  # every Paper/Folia slave where AFK detection should run.
  # Paper/Folia slaves detect inactivity; the proxy master applies kicks and time removal.
  # Do not change the value while the server is running!
  # The required classes will not be loaded if this option is false on startup.
  # If you change the value to false in runtime and reload the plugin, it could lead into issues with the afk detection.
  enabled: false

  # The time after which a player is considered AFK. You can use the unit-modifier for this.
  # Currently, the player will be considered afk after 15 minutes.
  after: '15m'

  # If true, the time that the player is afk will be removed.
  removeTime: true

  # If true, the player will be kicked after the time specified in 'kick.after'.
  autoKick: true

  # The time how often the plugin checks if a player is afk.
  # The unit for this is seconds.
  repeatCheck: 30
```

</details>

## Permissions

| Feature                                                                                                                  | Permission                          |
|--------------------------------------------------------------------------------------------------------------------------|-------------------------------------|
| The time will not be removed in case he is afk and the removeTime feature is enabled                                     | `loritime.afk.bypass.timeRemove`    |
| Prevents the kick if the player went afk                                                                                 | `loritime.afk.bypass.kick`          |
| Bypasses the AFK stop-count behavior, so the player's time keeps counting while AFK                                      | `loritime.afk.bypass.stopCount`     |
| Sends a message to all player with the permission, that the player went afk                                              | `loritime.afk.announce.afkAnnounce` |
| Sends a kick message to alle the player with the permission, that the player were kicked because he were afk for time xy | `loritime.afk.announce.kick`        |

## Time Entry Reasons

AFK handling can write dedicated session reasons:

| Situation | Reason |
|-----------|--------|
| The player becomes AFK, is not kicked, and does not have `loritime.afk.bypass.stopCount` | `PLAYER_AFK` |
| The player is kicked by AFK auto-kick enforcement | `PLAYER_AFK_KICK` |
| AFK removes already counted time through `afk.removeTime` | `AFK_ADJUSTMENT` |

AFK start and resume messages describe the player's AFK state. They are sent independently from whether `loritime.afk.bypass.stopCount` keeps the player's time counting.
