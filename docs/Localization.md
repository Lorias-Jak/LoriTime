LoriTime loads localization files from the plugin data folder at `language/<locale>.yml`.

The configured default language is controlled by `general.language` in `config.yml`. Bundled languages are created from LoriTime resources when needed. Custom languages are not copied from the jar: create the file yourself in the plugin data `language/` folder and set `general.language` to the file name without `.yml`.

LoriTime 2 development builds use a new localization schema. Legacy language files are not read by the new loader; regenerate or migrate custom files before using them.

```yml
schema_version: 1
locale: 'en-us'
prefix: '<#A4A4A4>[<#50CBAB>LoriTime<#A4A4A4>] '
messages:
  message:
    noPermission: '<#FF3232>You do not have permission.'
    command:
      loritime:
        notFound: '<#A4A4A4>The player <#50CBAB>[player] <#A4A4A4>has not played yet.'
  unit:
    second:
      singular: 'second'
      plural: 'seconds'
      identifier: ['s', 'sec']
```

Messages use MiniMessage formatting. Do not translate MiniMessage tags, command names, command arguments, or placeholders such as `[player]`, `[time]`, `[scope]`, and `[range]`.

On reload, LoriTime reloads the configured language, the hard fallback `en-us`, and languages that were already requested during the current plugin lifecycle. Unused YAML files in `language/` are not loaded just because they exist.
