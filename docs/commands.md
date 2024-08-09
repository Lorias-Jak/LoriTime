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
    <td class="tg-9wq8">/lttop &lt;page&gt;</td>
    <td class="tg-9wq8">ttop, lttop, ltop, toptimes</td>
    <td class="tg-9wq8">Get a list of players recognized by LoriTime, sorted by the most time spent online</td>
    <td class="tg-9wq8">loritime.top</td>
  </tr>
  <tr>
    <td class="tg-9wq8">/loritimeinfo</td>
    <td class="tg-9wq8">lti, linfo, ltimeinfo, loritinfo</td>
    <td class="tg-9wq8">Get some basic plugin infos</td>
    <td class="tg-9wq8">loritime.info</td>
  </tr>
  <tr>
    <td class="tg-9wq8">/loritimeadmin [set] [TimeString*]</td>
    <td class="tg-9wq8" rowspan="4">lta, ltadmin, loritimea</td>
    <td class="tg-9wq8">Set the time to the given time string</td>
    <td class="tg-9wq8" rowspan="4">loritime.admin</td>
  </tr>
  <tr>
    <td class="tg-9wq8">/loritimeadmin [modify] [TimeString*]</td>
    <td class="tg-9wq8">Adds or removes the time given in the time string</td>
  </tr>
  <tr>
    <td class="tg-9wq8">/loritimeadmin [delete]</td>
    <td class="tg-9wq8">Deletes all the time stored on a player</td>
  </tr>
  <tr>
    <td class="tg-9wq8">/loritimeadmin [reload]</td>
    <td class="tg-9wq8">Reloads the LoriTime plugin and config</td>
  </tr>
</tbody>
</table>


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
