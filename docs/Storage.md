LoriTime stores data in a database. You can choose between SQLite for a local file database and MySQL/MariaDB for a remote database.

## Storage Types

| Type | Description |
|------|-------------|
| `sqlite` | File-based SQLite database |
| `mysql` | Database hosted on a MySQL server |
| `mariadb` | Database hosted on a MariaDB server |

Only MariaDB and SQLite are tested regularly. Legacy `yml` storage is no longer a regular storage mode. If LoriTime detects `data/names.yml` or `data/time.yml` on startup, it migrates them to SQLite.

## Database Configuration

Set `storageMethod` in `config.yml`:

```yml
storageMethod: 'sqlite'

data:
  tablePrefix: 'loritime'
  host: 'localhost'
  port: 3306
  database: 'minecraft'
  user: 'user'
  password: 'pw'
```

The `data` section is only used for remote database storage methods, except `tablePrefix`, which is shared by all SQL backends.

## Storage Modes

LoriTime has three storage responsibility modes:

| Mode | Responsibility |
|------|----------------|
| `standalone` | The instance reads and writes its own canonical storage. |
| `master` | The instance owns canonical storage for a multi-setup and answers slave read requests. |
| `slave` | The instance sends session writes to a master and keeps a local read cache for local consumers. |

`multiSetup.mode` is the authoritative setting. The default is `standalone`.

```yml
multiSetup:
  mode: 'standalone'
```

Modes describe storage responsibility only. Platform modules decide what features they can provide in that mode.

## Platform Behavior

| Platform | `standalone` | `master` | `slave` | Context source |
|----------|--------------|----------|---------|----------------|
| Paper/Folia | Supported | Supported | Supported | Configured server name plus Bukkit world |
| Velocity | Supported | Supported | Not recommended | Backend server name plus `global` world |
| Bungee | Supported | Supported | Not recommended | Backend server name plus `global` world |

Paper and Folia-compatible servers can provide player name, configured server context, and world context for session rows. Set the logical server name on every Paper/Folia instance:

```yml
server:
  name: 'survival-1'
```

In a proxy network, this value should match the backend server name used by the proxy. The world value is read from the player's current Bukkit world.

Velocity and Bungee can derive backend server names from proxy server-switch events. Proxies do not know Bukkit worlds, so they store proxy-written rows with:

- server: backend server name
- world: `global`

In a multi-setup, Paper/Folia slave servers report their configured server name and current world to the master. This is the most precise setup for PlaceholderAPI and per-world time.

## Session Context Updates

Paper/Folia session context changes when the player's effective world changes. LoriTime ignores duplicate context events when the server and world did not change.

Velocity and Bungee session context changes when the player connects to a different backend server.

Flush and stop operations update the active session row so a crash preserves time up to the latest flush without splitting a continuous session into many rows.

## Storage Cleanup

Storage cleanup is disabled by default. When enabled, LoriTime deletes old time history for inactive players but keeps the player identity row.

```yml
storageCleanup:
  enabled: false
  inactiveAfterDays: 365
```

Cleanup removes session rows and manual adjustment rows for players whose activity timestamp is older than the configured threshold.
