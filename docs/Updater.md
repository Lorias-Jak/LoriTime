LoriTime has got the ability to update itself and download these updates from Modrinth or GitHub. This feature is enabled by default.

## Update Strategy

| Type  | Description                     | 
|-------|---------------------------------|
| MAJOR | The first digit of the version  |
| MINOR | The second digit of the version |
| PATCH | The third digit of the version  |

* A version is normally built like this: `MAJOR.MINOR.PATCH`. For example: `1.2.5`.
* You have the posibility to update the plugin automatically. You can enable this by setting `autoUpdate` to `true` in the `config.yml`. It will respect the Update Strategy
* There is currently not the option to get development updates. This is planned for the future.

> ⚠️ **Note:** At the moment the automatic file replacement is only available on Paper. On Bungee and Velocity an update folder will be created within the plugin directory, you have to move the files yourself.

> ⚠️ **Note:** Velocity is currently working on the update folder feature. BungeeCord doesn't want to introduce it so the auto update is kind of limited there.

## Configuration of the Updater
<details>
<summary>Updater configuration (config.yml)</summary>

```yml
###########
# Updater #
###########
updater:

  # LoriTime will automatically check for Updates on Modrinth or GitHub.
  # In case you use the multi-setup this will only be checked on the master.
  # If you want to disable this feature, set this to false.
  checkForUpdates: true

  # The Strategy for the update command.
  # Available options are 'MAJOR', 'MINOR', 'PATCH' in the format MAJOR.MINOR.PATCH (e.g. 1.2.5)
  updateStrategy: 'MINOR'

  # If the plugin should update itself automatically if a new release version is available.
  # This will respect the updateStrategy that you have set above.
  autoUpdate: false

  # If true, the plugin will send a message to the player if a new version is available.
  # The message will only appear on server join.
  # It will only be sent once every 24 hours or after a server restart.
  inGameNotification: true
```

</details>

## File Backups
If the plugin updates itself, there is a possibility that the config or language files will be updated. <br>
To prevent data loss, the plugin will create a backup of the files before updating them. The backups are stored in the `backups` folder in the plugin directory.<br>
There are several things you have to know about the backups and updated files:
* Once the maximum number of backups is reached, the oldest backup will be deleted. You can set the maximum number of backups in the `config.yml`.<br>
* You can disable the backup limit by setting the `maxBackups` to `0`.<br>
* The userdata will currently not be backed up.
* The Keys and Values of the config and language file will only update, if the key either is new or deleted. If the value within a key changes, it will not be updated at the moment.

<details>
<summary>Creating Backups (config.yml)</summary>

```yml
###########
# Backups #
###########
backup:

  # If true, the plugin will create backups every time the config or language files got an update.
  enabled: true

  # The maximum number of backups that will be stored.
  # If the number of backups exceeds this value, the oldest backup will be deleted.
  # Set this to 0 to disable the deletion of old backups.
  maxBackups: 5
```

</details>
