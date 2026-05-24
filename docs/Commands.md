LoriTime provides a number of different commands that you can use. This page lists the permissions for each command, so you can manage access to the different functions of the plugin.

## Commands & Permissions
<table class="tg">
<thead>
  <tr>
    <th class="tg-uzvj">Command</th>
    <th class="tg-uzvj">Aliases</th>
    <th class="tg-uzvj">Description</th>
    <th class="tg-uzvj">Permission</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td class="tg-9wq8">/loritime</td>
    <td class="tg-9wq8" rowspan="2">lt,lorit, ltime</td>
    <td class="tg-9wq8">To view your online time</td>
    <td class="tg-9wq8">loritime.see</td>
  </tr>
  <tr>
    <td class="tg-9wq8">/loritime &lt;player&gt;</td>
    <td class="tg-9wq8">To view the online time of the specified player</td>
    <td class="tg-9wq8">loritime.see.other</td>
  </tr>
  <tr>
    <td class="tg-9wq8">/loritimetop &lt;page&gt;</td>
    <td class="tg-9wq8">ttop, lttop, ltop, toptimes</td>
    <td class="tg-9wq8">Get a list of players recognized by LoriTime, sorted by the most time spent online</td>
    <td class="tg-9wq8">loritime.top</td>
  </tr>
  <tr>
    <td class="tg-9wq8">/ltinfo</td>
    <td class="tg-9wq8">lti, linfo, ltimeinfo</td>
    <td class="tg-9wq8">Get some basic plugin infos</td>
    <td class="tg-9wq8">loritime.info</td>
  </tr>
  <tr>
    <td class="tg-9wq8">/ltadmin [set] [player] [TimeString*]</td>
    <td class="tg-9wq8" rowspan="4">lta, loritimeadmin, loritimea</td>
    <td class="tg-9wq8">Set the time to the given time string</td>
    <td class="tg-9wq8" rowspan="4">loritime.admin</td>
  </tr>
  <tr>
    <td class="tg-9wq8">/ltadmin [modify] [player] [TimeString*]</td>
    <td class="tg-9wq8">Adds or removes the time given in the time string</td>
  </tr>
  <tr>
    <td class="tg-9wq8">/ltadmin [reset] [player]</td>
    <td class="tg-9wq8">Resets all the time stored on a player</td>
  </tr>
  <tr>
    <td class="tg-9wq8">/ltadmin [deleteUser] [Playername] confirm</td>
    <td class="tg-9wq8">Deletes the user entirely from LoriTime</td>
  </tr>
  <tr>
    <td class="tg-9wq8">/ltserver reload</td>
    <td class="tg-9wq8">Reloads the local LoriTime instance and config</td>
    <td class="tg-9wq8">loritime.admin</td>
  </tr>
  <tr>
    <td class="tg-9wq8">/ltadmin [update]</td>
    <td class="tg-9wq8">Updates the plugin if an update is available</td>
    <td class="tg-9wq8">loritime.admin</td>
  </tr>
  <tr>
      <td class="tg-9wq8">/ltdebug</td>
      <td class="tg-9wq8">loritimedebug</td>
      <td class="tg-9wq8">Enable or disable the debugger.</td>
      <td class="tg-9wq8">loritime.debug</td>
  </tr>
  <tr>
      <td class="tg-9wq8">/afk</td>
      <td class="tg-9wq8">/</td>
      <td class="tg-9wq8">Set yourself AFK or not</td>
      <td class="tg-9wq8">loritime.afk</td>
  </tr>
  
</tbody>
</table>

<br>

> **Note:** The debugger will be turned off automatically after the configured time. Be aware that you need to enable the debugger via the console if you use MultiSetup.<br>

> **Note:** `reload`, `ltdebug`, and `ltinfo` are local operational commands. In a multi-setup, execute reload on each proxy/backend instance that should reload.

> **Note:** Canonical data commands such as time lookup, top list, and admin time edits are available on proxy storage owners and backend `standalone`/`master` instances. Backend `slave` instances only register local operational commands and AFK when enabled.

> **Note:** You can customize command names and aliases in `commands.yml`. Command availability is still decided by LoriTime's runtime profile so unsupported commands are not registered on the wrong instance type.
<details>
<summary>Custom command alias</summary>

```yml
profiles:
  proxy:
    admin:
      name: 'ltadmin'
      aliases: ['ltap', 'loritimeadmin', 'loritimeproxyadmin']
    local:
      name: 'ltserver'
      aliases: ['lts', 'ltlocal']
  backend:
    canonical:
      admin:
        name: 'ltadmin'
        aliases: ['lta', 'loritimeadmin', 'loritimea']
    slave:
      local:
        name: 'ltserver'
        aliases: ['lts', 'ltlocal']
```

</details>

Existing alias customizations from `config.yml` should be moved to the matching command node in `commands.yml`. Use `profiles.proxy` for Bungee/Velocity, `profiles.backend.canonical` for backend `standalone` or `master`, and `profiles.backend.slave` for backend `slave`.

<br>


## TimeString Examples
The TimeString is a special way to set, add or remove the time on the player. The exact identifiers are written in the config and can be customized for personal use. Below are a few examples of exactly how setting, modifying and subtracting times might look. <br>
* just any whole number, default seconds <br>
* multiple combinations of amount with unit <br>
<br>

| TimeString examples | Effect                                 |
|---------------------|----------------------------------------|
| `77`                | 77 seconds or 1 minute and 17 seconds. |
| `4h 3min`           | 4 hours and 3 minutes                  |
| `28d 1h`            | 28 days and 1 hour                     |
| `2w1d`              | 2 weeks and one day                    |
| `1h -5min`          | 1 hour minus 5 minutes or 55 minutes   |
| `-6d`               | minus 6 days (only for modify usage)   |
