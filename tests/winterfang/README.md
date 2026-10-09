# Winterfang checks

Run the standalone test using the commands in [development](../../docs/development.md). It covers control durations, upgraded Pain ammunition counts, unique target reservations, and exhaustion without target reuse.

`check_freeze_hooks.py` inspects the packaged common freeze mixins against the matching mapped Minecraft jar. For Fabric it resolves the packaged common refmap before checking intermediary targets. It checks method descriptors, not Mixin execution.

Gameplay acceptance checks, including player packet rejection, status-immune bosses, damage immunity timing, harmless tipped/spectral Grace arrows, and shell appearance, are listed in [testing](../../docs/testing.md#winterfang-redesign-checks).
