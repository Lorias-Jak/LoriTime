# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - ${maven.build.timestamp}
### Added
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
