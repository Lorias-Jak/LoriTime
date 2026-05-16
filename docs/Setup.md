This will walk you through installing HuskHomes on your Spigot, Fabric or Sponge server, or proxy network of servers.

## Requirements 
* To see all requirements pls checkout the [compatibility](https://github.com/Lorias-Jak/LoriTime/wiki/Compatibility) page.
* Download the jar file for your server from the [latest release](https://github.com/Lorias-Jak/LoriTime/releases/latest).

## When to use MultiSetup?
The MultiSetup is a realization for networks within a single-proxy and subserver system. 
LoriTime is communicating between the proxy and subservers to send data between them.
Not every data is available on the proxy for every feature.
<br>
<br>
You **should not** use MultiSetup if:
- You're only having a Bukkit server without a proxy
- You dont need Placeholder on your Subserver
- You dont want to use the afk feature

You **should** use MultiSetup if:
- You want to use Placeholder
- You want to use the AFK feature

> **Note:** There is currently no support for multi-proxy setups!

## Setup Instructions - Single Server
### 1. install the jar file
- Put the jar file of the plugin in the `plugins` directory on your server.
### 2. start and stop the server.
- When you start it, it will create all the required files for you.
- You can now edit the configuration file and localization if needed.
### 3. start your server again.
- The plugin should now load normally and you can enjoy LoriTime!

## Setup Instructions - Multi-Server
### 1. install the jar file
- Put the jar file of the plugin in the `plugins` directory on your Proxy and Sub-Server
### 2. start and stop the server.
- When you start it, it will create all the required files for you.
- You can now edit the configuration file and localization if needed.
### 3. Configure the configs
- It's important that you configure the configs on its own. You can nearly copy-paste the configuration per server. Not every Parameter is used on Proxy or on the subserver.
- Set the Proxy `multiSetup.mode` to `master`
- Set each Paper/Folia Sub-Server `multiSetup.mode` to `slave`
- The proxy backend server name is used for canonical session rows. Paper/Folia slave `server.name` is not used for canonical server entries in this setup.
### 4. start your server again.
- The plugin should now load normally and you can enjoy LoriTime!

<details>
<summary>Multi-Setup Proxyside(config.yml)</summary>

```yml
##############
# MultiSetup #
##############
multiSetup:

  # Options to set: "standalone", "master", "slave".
  mode: 'master'
```

</details>

<details>
<summary>Multi-Setup Subserver(config.yml)</summary>

```yml
##############
# MultiSetup #
##############
multiSetup:

  # Options to set: "standalone", "master", "slave".
  mode: 'slave'

server:
  # Used only when this Paper/Folia instance owns local session rows.
  # In proxy slave mode, the proxy backend name is canonical instead.
  name: 'survival-1'
```

</details>
