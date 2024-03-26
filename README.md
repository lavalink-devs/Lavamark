# Lavamark
Lavaplayer server benchmarker. How many lavaplayer tracks can your server handle playing at once?

[Download](https://ci.fredboat.com/viewType.html?buildTypeId=Public_Lavamark_Build&guest=1)

## Usage
```
java -jar lavamark-1.0.jar
```

## Lavamark Options
You can specify any of the following options as a command-line argument when launching Lavamark.

| Option           | Description                                                                                |
|------------------|--------------------------------------------------------------------------------------------|
| `h`/`help`       | Displays Lavamark's available options.                                                     |
| `i`/`identifier` | The identifier/URL of the track/playlist to use for the test.                              |
| `b`/`block`      | The IPv6 block to use for rotation, specified as CIDR notation.                            |
| `s`/`step`       | The number of players to spawn after a fixed interval. Be careful when using large values. |
| `t`/`transcode`  | Simulate a load by forcing transcoding.                                                    |
