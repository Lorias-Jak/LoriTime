Currently LoriTime has two ways to store your data. The first is via .yml files, the other is via a database.<br>
Choose what suits you more, there are no restrictions by your choice<br>

## Types
> ⚠️ Note that you cannot migrate between different storage types!<br>
> ⚠️ Only MariaDB is tested, not mysql. In case you have any problems, pls report them<br>

| Type | Description | 
|------|-------------|
| yml | A file-based Storage |
| mysql | A database hosted on a MySQL or MariaDB server |
| sqlite | A file-based SQLite database |

## Setting up the database
<p>To change the storage type, open your `config.yml` and enter all database properties.</p>
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
  dialect: 'mariadb' # options: mariadb, mysql

###########
# SQLite  #
###########
sqlite:
  file: 'loritime.db'
  tablePrefix: 'loritime'
```

</details>
