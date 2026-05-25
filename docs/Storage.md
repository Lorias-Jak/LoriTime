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
| `slave` | The instance reports slave-owned context or writes to a master and keeps a local read cache for local consumers. |

`multiSetup.mode` is the authoritative setting. The default is `standalone`.

```yml
multiSetup:
  mode: 'standalone'
```

Modes describe storage responsibility only. Platform modules decide what features they can provide in that mode.

## Runtime Threading

LoriTime storage contracts are internal synchronous contracts, but normal runtime database-backed reads and writes are scheduled away from platform main-thread request and tick paths. Commands, listeners, AFK handling, plugin messaging, and periodic cache flushing perform storage work through LoriTime's async scheduler paths.

Synchronous request surfaces cannot wait on database calls. Command tab completion uses known runtime player names from cache and online players. Paper/Folia PlaceholderAPI rendering uses cached time values, returns `0` on cache miss, and requests an asynchronous refresh for later renders.

Startup migration and storage initialization are lifecycle exceptions. They may run synchronously during plugin enable before normal runtime ticking is expected to use LoriTime storage.

## Platform Behavior

| Platform | `standalone` | `master` | `slave` | Context source |
|----------|--------------|----------|---------|----------------|
| Paper/Folia | Supported | Supported | Supported | Configured server name plus Bukkit world outside proxy-owned sessions; current Bukkit world only as a proxy slave |
| Velocity | Supported | Supported | Not recommended | Backend server name plus latest slave-reported world or `global` fallback |
| Bungee | Supported | Supported | Not recommended | Backend server name plus latest slave-reported world or `global` fallback |

Paper and Folia-compatible servers can provide player name, configured server context, and world context for session rows. Set the logical server name on every Paper/Folia instance:

```yml
server:
  name: 'survival-1'
```

In a proxy multi-setup where the proxy runs as `master` and Paper/Folia servers run as `slave`, this value does not create canonical server entries. The proxy backend server name is the canonical server context, and the Paper/Folia slave reports only the player's current Bukkit world to enrich the master-owned active row.

Velocity and Bungee can derive backend server names from proxy server-switch events. Proxies store proxy-written rows with:

- server: backend server name
- world: latest Paper/Folia slave-reported world when available, otherwise `global`

In a multi-setup, Paper/Folia slave servers report current world context to the master. They do not report completed session chunks and do not create separate canonical server entries.

## Session Context Updates

Paper/Folia standalone or master session context changes when the player's effective world changes. Paper/Folia slave world changes update the current world context on the proxy master without creating a new time row.

Velocity and Bungee session context changes when the player connects to a different backend server.

Flush and stop operations update the active session row so a crash preserves time up to the latest flush without splitting a continuous session into many rows.

## Scoped Time

LoriTime can query and adjust time globally, for a server, or for one world on one server.

- Global totals include all session rows and all manual adjustments for the player.
- Server totals include sessions on that server, server-scoped adjustments, and world-scoped adjustments for worlds on that server. Global adjustments are not distributed into server totals.
- World totals include sessions and world-scoped adjustments for the exact server/world pair. Global and server-scoped adjustments are not distributed into world totals.

AFK time removal writes an adjustment for the player's current world when the runtime knows the current server/world context. If no specific context is available, LoriTime uses the shared fallback server/world context.

Time-ranged lookups use the same scope rules. Persisted sessions are clipped to the requested inclusive-start/exclusive-end window, and manual adjustments count when their `created_at` audit timestamp is inside the window. A ranged lookup with no overlapping rows returns no stored time instead of a zero-valued hit.

This storage layout is a breaking database baseline change for LoriTime 2 development builds. Existing normalized databases with unscoped adjustment rows are not migrated by this change.

## Storage Cleanup

Storage cleanup is disabled by default. When enabled, LoriTime deletes old time history for inactive players but keeps the player identity row.

```yml
storageCleanup:
  enabled: false
  inactiveAfterDays: 365
```

Cleanup removes session rows and manual adjustment rows for players whose activity timestamp is older than the configured threshold.
