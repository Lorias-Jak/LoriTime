This will walk you through installing HuskHomes on your Spigot, Fabric or Sponge server, or proxy network of servers.

## Requirements 
* To see all requirements pls checkout the [compatibility](https://github.com/Lorias-Jak/LoriTime/wiki/Compatibility) page.
* Download the jar file for your server from the [latest release](https://github.com/Lorias-Jak/LoriTime/releases/latest).

## When to use on MultiSetup?
The MultiSetup is a realization for networks within a single-proxy and subserver system. 
LoriTime is communicating with between the proxy and subservers to send data between them.
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
- Set the Proxy `multiSetup.enabled` to `true` and the `multiSetup.mode` to `master`
- Set the Sub-Server `multiSetup.enabled` to `true` and the `multiSetup.mode` to `slave`
### 4. start your server again.
- The plugin should now load normally and you can enjoy LoriTime!

<details>
<summary>Multi-Setup Proxyside(config.yml)</summary>

```yml
##############
# MultiSetup #
##############
multiSetup:

  # If true, the plugin will be enabled for multiple server setup.
  # Only activate and set on master on proxy-server, use the slave mode on sub-servers.
  enabled: true

  # Options to set: "master", "slave".
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

  # If true, the plugin will be enabled for multiple server setup.
  # Only activate and set on master on proxy-server, use the slave mode on sub-servers.
  enabled: true

  # Options to set: "master", "slave".
  mode: 'slave'
```

</details>
