## API

The LoriTime API offers various methods for querying and managing the online times of players.

## Compatibility
| Plattform | Version         | Supported |
|-----------|-----------------|-----------|
| paper     | 1.7.0 - current | ✅         |
| Folia     | 1.7.0 - current | ✅         |
| velocity  | 1.7.0 - current | ✅         |
| bungee    | 1.7.0 - current | ✅         |

## API Introduction

> **Note:** There is currently no Maven or Gradle repo! This could be added in the future if it's really necessary.

<details>
<summary>1. Adding LoriTime as a dependency</summary>

```yml
name: MyPlugin
version: 1.0
main: myplugin.MyPlugin
author: MaxMustermann
description: 'A plugin that hooks with the LoriTime API!'
softdepend: # or 'depend'
  - LoriTime
```

</details>

<details>
<summary>2. Creating a class to do stuff with LoriTime</summary>

```java
public class LoriTimeAPIHook {
  
    public LoriTimeAPIHook() {
        // Ready to do stuff with the API
    }

}
```

</details>

<details>
<summary>3. Instancing the plugin Hook</summary>

```java
public class MyPlugin extends JavaPlugin {
    private LoriTimeAPIHook loriTimeHook;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("LoriTime") != null) {
            this.loriTimeHook = new LoriTimeAPIHook();
        }
    }
}
```

</details>

* You can now get the API instance by calling LoriTimeAPI#get()
