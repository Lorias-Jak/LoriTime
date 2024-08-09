LoriTime supports a number of collaborative translations of the plugin locales into different languages. The default language is [`en`](https://github.com/Lorias-Jak/LoriTime/blob/main/src/main/resources/localization/en.yml) (English). The message file is formatted with [MiniMessage](https://docs.advntr.dev/minimessage/format.html#standard-tags).

You can change the language to use by changing the `general.language` key in the config.yml file of the plugin. You must change this setting to one of the supported languages. You can view a list of [supported languages](https://github.com/Lorias-Jak/LoriTime/tree/main/src/main/resources/localization) by looking at the locales source folder. The key is the file-name.

A special feature of the localization are the adjustable identifiers of the time strings. The identifiers are used to recognize whether the time units seconds, minutes, hours or others are meant. In the example below it is indicated that months have the identifier `mo`, weeks the identifier `w`, seconds the identifiers `s` and `sec`. These are to be changed as desired. Please note that the identifiers must be unique and not the same!

<details>
<summary>Identifier in Locale (en.yml)</summary>

```yml
unit:
  second:
    singular: 'second'
    plural: 'seconds'
    identifier:
      - 's'
      - 'sec'
  minute:
    singular: 'minute'
    plural: 'minutes'
    identifier:
      - 'min'
      - 'm'
  hour:
    singular: 'hour'
    plural: 'hours'
    identifier:
      - 'h'
  day:
    singular: 'day'
    plural: 'days'
    identifier:
      - 'd'
  week:
    singular: 'week'
    plural: 'weeks'
    identifier:
      - 'w'
  month:
    singular: 'month'
    plural: 'months'
    identifier:
      - 'mo'
  year:
    singular: 'year'
    plural: 'years'
    identifier:
      - 'y'
      - 'yhr'
```

</details>

## Contribute locales
You can contribute locales by submitting a pull request with a yml file containing translations of the [default locales](https://github.com/Lorias-Jak/LoriTime/blob/main/src/main/resources/localization/en.yml) to your language. Here are a few hints on how to do this: 
* Do not translate the locale keys themselves (e.g. `teleporting_offline_player`).
* Your pull request should be for a file in the [locales folder](https://github.com/Lorias-Jak/LoriTime/tree/main/src/main/resources/localization).
* Do not translate the [MiniMessage](https://docs.advntr.dev/minimessage/format.html#standard-tags) syntax itself or commands and their parameters; only the English interface text.
* Each locale should be on one line, and the header line should be removed.
* Use the correct ISO 639-1 [locale code](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes) for your language and dialect.
