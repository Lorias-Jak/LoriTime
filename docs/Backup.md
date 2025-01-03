## Configuration
* Set `backup.enabled` to `true`
* Set `backup.maxBackups` to limit the amount of backups. Set to 0 to disable the backup limit and keep all backups.

> ⚠️If you disable the maximum amount of backups, the plugin will create a new backup every time the server starts. This can lead to a lot of backups and can fill up your storage space.<br>

Sadly the whole styling of the config and language file will be lost after the first automated config update.
This is a compromise to have an automated file update system. <br><br>
If you want to keep the style of the config file, you have to regenerate the config and language file each time you update the plugin.

## The config part
<details>
<summary>Backup-Config (config.yml)</summary>

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
