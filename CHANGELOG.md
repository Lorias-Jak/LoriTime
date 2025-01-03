# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - ${maven.build.timestamp}
### Added
- Auto updater for the plugin config and language file
- Backup system for the plugin config and language file. New keys in the config file are automatically added to the backup file. The config will lose its style after the first automated config backup. This is a compromise to have an automated config update system.
  - new key `backup.enabled` to enable or disable the backup system.
  - new key `backup.maxBackups` to limit the amount of backups. Set to 0 to disable the backup limit.
### Changed
### Deprecated
### Removed
### Fixed
### Security

## [1.6.0] - 2024-12-26
### Added
- Paper plugin loader.
- Folia platform loader.
- `deleteUser` argument to `/lta` command to delete a user entirely from the plugin.
### Changed
- `delete` argument from the `/lta` command to `reset`.
### Deprecated
### Removed
### Fixed
### Security

## [1.5.0] - 2024-09-29
### Added
- `/ltdebug` command to enable or disable the console debugger.
- new placeholder representing the total amount of time the player has been online. The values are rounded to two decimal places. 
  - `%loritime_seconds_total%`
  - `%loritime_minutes_total%`
  - `%loritime_hours_total%`
  - `%loritime_days_total%`
  - `%loritime_weeks_total%`
  - `%loritime_months_total%`
  - `%loritime_years_total%`
### Changed
- Reworked the plugin logger for better logging
### Deprecated
### Removed
- Bukkit compatibility to remove bloated code. Only Paper is supported via the Bukkit loader.
### Fixed
### Security

## [1.4.4] - 2024-09-10
### Added
### Changed
### Deprecated
### Removed
### Fixed
- AFK feature on multi-setup sending as much messages as players are online
- AFK feature removing to much of the online time if the player went afk 
### Security

## [1.4.3] - 2024-09-07
### Added
- new Placeholder `%loritime_seconds%`, `%loritime_minutes%`, `%loritime_hours%`, `%loritime_days%`, `%loritime_weeks%`, `%loritime_months%`, `%loritime_years%`
- automatic docs build
- zh-cn translation (thanks to cygbs)
### Changed
- the way how the PluginMessaging is working within the plugin. No notable changes for the user
### Deprecated
### Removed
### Fixed
- Placeholder not properly working on multisetup
### Security

## [1.4.2] - 2024-07-08
### Added
- more metrics for better tracking what features are used
### Changed
- to only show `release` updates from modrinth for the users.
- how the version gets displayed when using the info command
### Deprecated
### Removed
### Fixed
- wrong versioning of the velocity module
- error message when deactivating the plugin if the cache scheduler is not running
- cache task running as `slave` in a multi-setup, even if its not needed.
- storage being loaded as `slave` in a multi-setup, even if its not needed.
### Security
### To update
- Just drag and drop the new jar in.
