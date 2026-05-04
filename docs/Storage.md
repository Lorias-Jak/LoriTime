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

If `multiSetup.enabled` is `false`, LoriTime runs as `standalone`. If `multiSetup.enabled` is `true`, `multiSetup.mode` must be one of `standalone`, `master`, or `slave`.

Modes describe storage responsibility only. Platform modules decide what features they can provide in that mode.

## Platform Behavior

Paper and Folia-compatible servers can provide player name, server, and world context for session rows. They are also the only supported target for PlaceholderAPI placeholders.

Velocity and Bungee can run as `standalone` or `master`, but they cannot provide PlaceholderAPI placeholders because PlaceholderAPI is not a proxy plugin. When a proxy writes session rows without world context, LoriTime stores them in the fallback scope:

- server: `default`
- world: `global`

In a multi-setup, the slave servers are the instances that need placeholders. The master owns canonical storage; it does not need PlaceholderAPI for slave placeholder values.

## Placeholder TODO

Paper/Folia slave placeholder support is intentionally deferred in the unified storage refactor. The placeholder integration still needs to be updated to read from the slave read cache and to return deterministic values on cache misses while requesting a refresh from the master.
