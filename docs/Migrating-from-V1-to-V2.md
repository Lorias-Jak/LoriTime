LoriTime 2 changes storage internals and tightens AFK behavior. Read this before upgrading a production server from LoriTime 1.x.

## Before Updating

1. Stop every server or proxy instance that writes LoriTime data.
2. Back up your plugin data folder and your SQL database if you use MySQL or MariaDB.
3. Update LoriTime on all instances before starting the network again.

## Storage Migration

LoriTime 2 uses normalized SQL storage for player identity, sessions, worlds, servers, and manual adjustments.

Development builds with scoped server/world time support change the normalized manual adjustment schema. This is a breaking database baseline change: existing normalized LoriTime 2 development databases with unscoped adjustment rows are not migrated by this change.

Legacy flat-file storage is no longer a regular storage mode. If LoriTime detects `data/names.yml` or `data/time.yml`, it backs up the files, imports their data into SQLite, and renames the source files with a `.migrated` suffix after a successful import.

Existing LoriTime 1.x SQL databases are detected before normal storage startup. The updater routes them through the versioned migration path and imports legacy aggregate time into the new schema using the configured fallback server and world context.

## Storage Configuration

Set the storage backend with `storageMethod`:

```yml
storageMethod: 'sqlite'
```

The supported regular storage methods are:

| Type | Description |
|------|-------------|
| `sqlite` | Local SQLite database file |
| `mysql` | Remote MySQL database |
| `mariadb` | Remote MariaDB database |

`yml` is no longer a regular runtime storage method.

## Multi-Setup Mode

LoriTime 2 uses `multiSetup.mode` as the authoritative storage responsibility setting:

```yml
multiSetup:
  mode: 'standalone'
```

Use:

| Mode | Use when |
|------|----------|
| `standalone` | This instance owns its local LoriTime storage. |
| `master` | This instance owns canonical storage for a multi-setup. |
| `slave` | This instance reports data to a master and uses a local read cache. |

## AFK Permission Change

The meaning of `loritime.afk.bypass.stopCount` changed in LoriTime 2.

| Version | Behavior |
|---------|----------|
| LoriTime 1.x | Having the permission caused LoriTime to stop counting time while AFK. |
| LoriTime 2 | Having the permission bypasses the stop-count behavior, so time keeps counting while AFK. |

Default LoriTime 2 behavior is now: when a player becomes AFK and is not kicked, LoriTime stops counting their online time. Give `loritime.afk.bypass.stopCount` only to players whose time should continue counting while AFK.

The player still receives the AFK self message when this bypass permission keeps their time counting.

AFK start and resume messages are no longer tied to this permission. A non-kicked player receives the AFK self message when becoming AFK and the AFK resume message when returning, whether time counting stopped or continued.

## AFK Session Reasons

LoriTime 2 records separate session reasons for AFK stops:

| Situation | Reason |
|-----------|--------|
| Player becomes AFK and time counting stops | `PLAYER_AFK` |
| Player is kicked by AFK auto-kick enforcement | `PLAYER_AFK_KICK` |

Existing historical rows are not migrated to `PLAYER_AFK_KICK`. The new reason is used for new AFK kicks after updating.

## After Updating

After the first successful migration startup, regenerate all LoriTime-managed configuration and localization files. The automated migration preserves values where possible, but rewritten files lose their original formatting and comments; regenerating `config.yml` and language files gives you the clean LoriTime 2 templates to reapply your settings to.

Then check startup logs for migration messages, and verify:

- The selected `storageMethod` is correct.
- `multiSetup.mode` is correct on every instance.
- AFK bypass permissions match the new LoriTime 2 behavior.
- Placeholder and command results show the expected migrated playtime.
