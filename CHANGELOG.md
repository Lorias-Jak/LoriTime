# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - ${maven.build.timestamp}
### Added
### Changed
### Deprecated
### Removed
### Fixed
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
