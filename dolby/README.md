# Dolby settings (LenovoDolby)

Settings app for the Dolby DAX3 DAP effect, imported from AOSPA's XiaomiDolby
(`swiitch-OFF-Lab/hardware_dolby`, branch `XiaomiDolby`, commit `bcc8cab`),
Apache-2.0. Local changes:

- module renamed to `LenovoDolby`;
- profiles limited to the ones in Lenovo's `dax-default.xml` (Dynamic, Movie,
  Music), the Voice profile (8) is not tuned here;
- IEQ preset values mapped to Lenovo's ids (1 detailed, 2 balanced, 3 warm);
- `EqualizerScreen` uses `MaterialTheme` (SpaLib on Android 16 dropped
  `SettingsTheme.colorScheme`).

The DAP effects, DMS libraries and `libdapparamstorage` come from the Motorola
DAX 3.6 set of the same repository: the stock Lenovo DAX 3.5 DAP ignores
parameter writes from this app. See `vendor/lenovo/tb9707f/proprietary-files.txt`.
