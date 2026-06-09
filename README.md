# LoriTime

[![License: GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-25-orange.svg)](pom.xml)
[![Minecraft](https://img.shields.io/badge/minecraft-1.21%2B-brightgreen.svg)](docs/Compatibility.md)
[![Platform](https://img.shields.io/badge/platform-Paper%20%7C%20Folia%20%7C%20Velocity-5865f2.svg)](docs/Compatibility.md)

LoriTime tracks how long players are connected to a Minecraft server or network. It supports standalone Paper/Folia servers as well as Velocity-based networks with proxy-owned storage and backend context reporting.

## Features

- Tracks playtime globally, per server, per world, and inside time ranges.
- Supports Paper, Folia, and Velocity runtimes.
- Provides single-server and proxy/backend multi-setup modes.
- Stores data in SQLite, MySQL, or MariaDB.
- Includes AFK handling, configurable commands, placeholders, localization, backups, and update checks.

> LoriTime 2 development builds require Java 25. See [Compatibility](docs/Compatibility.md) for the full platform matrix.

## Installation

1. Download the latest build from [Modrinth](https://modrinth.com/plugin/loritime#download) or [GitHub](https://github.com/Lorias-Jak/LoriTime/releases).
2. Put the matching plugin jar into the `plugins` directory of every server or proxy that should run LoriTime.
3. Start and stop the server once so LoriTime can create its configuration files.
4. Configure `config.yml`, `commands.yml`, and localization files as needed.
5. Start the server again.

For proxy networks, run Velocity as `master` and Paper/Folia backends as `slave` when the proxy should own canonical storage. See [Setup](docs/Setup.md) and [Storage](docs/Storage.md) for the exact configuration.
