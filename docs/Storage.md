Currently LoriTime stores data in a database.<br>
You can choose between SQLite (file-based) and MySQL/MariaDB (server-based).<br>

## Types
> ⚠️ Only MariaDB and SQLite are tested, not mysql. In case you have any problems, pls report them<br>

| Type | Description | 
|------|-------------|
| mysql | A database hosted on a MySQL server |
| mariadb | A database hosted on a MariaDB server |
| sqlite | A file-based SQLite database |

> ℹ️ Legacy `yml` storage is no longer a regular storage mode. If detected on startup,
> LoriTime automatically migrates `data/names.yml` and `data/time.yml` to SQLite.

## Setting up the database
<p>To change the storage type, open your `config.yml`, set <code>general.storage</code> to <code>mysql</code>, <code>mariadb</code> or <code>sqlite</code>, and enter all database properties.</p>
<details>
<summary>Database properties (config.yml)</summary>

```yml
###########
#  Mysql  #
###########
mysql:
  host: 'localhost'
  port: 3306
  database: 'test'
  user: 'user'
  password: '123ABC!'
  tablePrefix: 'loritime'

###########
# SQLite  #
###########
sqlite:
  file: 'loritime.db'
  tablePrefix: 'loritime'
```

</details>

## Proxy-only usage with database storage
If LoriTime runs only on a proxy (multi-setup master on Velocity / Bungee) and no world-specific source is available,
LoriTime stores accumulated time in a synthetic scope:

- server: `default`
- world: `global`

This keeps proxy-only deployments compatible with the normalized database schema, because no paper world data is required.
